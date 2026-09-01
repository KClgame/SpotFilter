package kcl.spotfilter.client.data

import kcl.spotfilter.client.audio.SpotSounds
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.minecraft.client.Minecraft

object SpotPool {
	private const val DEPLETE_RANGE = 48.0
	private val depleteRangeSq = DEPLETE_RANGE * DEPLETE_RANGE
	private const val WAVE_WINDOW_MS = 2000L
	private const val WAVE_THRESHOLD = 3

	private val spots = LinkedHashMap<SpotKey, FishingSpot>()
	private var nextId = 1
	private var lastNormalRefreshHour: Int? = null
	private var waveStartMs = 0L
	private var waveChanges = 0

	fun all(): Collection<FishingSpot> = spots.values

	fun pinned(): List<FishingSpot> {
		FilterState.refreshRanks()
		return FilterState.sortSpots(
			spots.values.filter { it.pinned && it.kind == FilterState.kind },
			FilterState.kind
		)
	}

	fun get(key: SpotKey): FishingSpot? = spots[key]

	fun byId(id: Int): FishingSpot? = spots.values.firstOrNull { it.id == id }

	fun byRank(rank: Int, kind: SpotKind = FilterState.kind): FishingSpot? {
		FilterState.refreshRanks()
		return FilterState.sortSpots(spots.values.filter { it.kind == kind }, kind)
			.firstOrNull { it.rank == rank }
	}

	fun upsert(incoming: FishingSpot) {
		if (incoming.kind == SpotKind.NORMAL) {
			replaceNormalColumn(incoming)
		}
		val existing = spots[incoming.key]
		if (existing == null) {
			incoming.id = nextId++
			spots[incoming.key] = incoming
			SpotSounds.playNewSpot()
			AutoPin.apply(incoming)
		} else {
			val becameDepleted =
				incoming.stock == StockLevel.DEPLETED && existing.stock != StockLevel.DEPLETED
			val previousFingerprint = existing.contentFingerprint()
			existing.entityId = incoming.entityId
			existing.x = incoming.x
			existing.y = incoming.y
			existing.z = incoming.z
			existing.stock = incoming.stock
			existing.stockRgb = incoming.stockRgb
			existing.perks = incoming.perks
			existing.lastSeenGameTime = incoming.lastSeenGameTime
			existing.kind = incoming.kind
			existing.stability = incoming.stability
			existing.stabilityRgb = incoming.stabilityRgb
			existing.stabilityRange = incoming.stabilityRange
			if (existing.kind == SpotKind.NORMAL && previousFingerprint != incoming.contentFingerprint()) {
				noteNormalWave()
			}
			if (becameDepleted && shouldKickDepleted()) {
				setPinned(existing, false)
			}
			if (existing.pinned) {
				PinnedSpotMarker.sync(existing)
			}
		}
	}

	fun setPinned(spot: FishingSpot, pinned: Boolean) {
		spot.pinned = pinned
		if (!pinned) {
			spot.autoPinned = false
			spot.pinColorOverride = null
			assignGroup(spot, null)
			PinnedSpotMarker.remove(spot.id)
		} else {
			PinnedSpotMarker.spawnOrUpdate(spot)
		}
	}

	fun assignGroup(spot: FishingSpot, nickname: String?) {
		val nick = nickname?.trim().orEmpty()
		if (nick.isEmpty()) {
			spot.nickname = null
			spot.groupIndex = 0
			return
		}
		if (spot.nickname == nick && spot.groupIndex > 0) return
		spot.nickname = nick
		spot.groupIndex = (spots.values
			.filter { it !== spot && it.nickname == nick }
			.maxOfOrNull { it.groupIndex } ?: 0) + 1
	}

	fun remove(key: SpotKey) {
		val removed = spots.remove(key) ?: return
		if (removed.pinned) {
			PinnedSpotMarker.remove(removed.id)
		}
	}

	fun clearSpots() {
		PinnedSpotMarker.removeAll()
		spots.clear()
		nextId = 1
	}

	fun clearKind(kind: SpotKind) {
		val keys = spots.filter { it.value.kind == kind }.keys.toList()
		keys.forEach { remove(it) }
	}

	fun tickNormalClockReset() {
		val now = java.time.LocalTime.now()
		val hour = now.hour
		if (now.minute < 1) return
		val previous = lastNormalRefreshHour
		lastNormalRefreshHour = hour
		if (previous != null && previous != hour) {
			clearKind(SpotKind.NORMAL)
			notifyRefresh("SpotFilter: island spots refreshed")
		}
	}

	fun finishNormalScan(seen: Set<SpotKey>) {
		if (waveChanges < WAVE_THRESHOLD) return
		val stale = spots.filter { it.value.kind == SpotKind.NORMAL && it.key !in seen }.keys.toList()
		if (stale.isEmpty()) {
			waveChanges = 0
			return
		}
		stale.forEach { remove(it) }
		waveChanges = 0
		notifyRefresh("SpotFilter: island spots refreshed")
	}

	fun refreshGrottoFromChat() {
		clearKind(SpotKind.GROTTO)
		notifyRefresh("SpotFilter: grotto spots refreshed")
	}

	private fun replaceNormalColumn(incoming: FishingSpot) {
		val stale = spots.filter { (_, spot) ->
			spot.kind == SpotKind.NORMAL &&
				spot.key.dimension == incoming.key.dimension &&
				spot.x == incoming.x &&
				spot.z == incoming.z &&
				spot.y != incoming.y
		}.keys.toList()
		if (stale.isNotEmpty()) noteNormalWave()
		stale.forEach { remove(it) }
	}

	private fun noteNormalWave() {
		val now = System.currentTimeMillis()
		if (now - waveStartMs > WAVE_WINDOW_MS) {
			waveStartMs = now
			waveChanges = 0
		}
		waveChanges++
	}

	fun kickDepletedNow() {
		spots.values.filter { it.stock == StockLevel.DEPLETED && it.pinned }.toList().forEach { setPinned(it, false) }
	}

	fun shouldKickDepleted(): Boolean = kcl.spotfilter.client.config.SpotFilterConfig.instance.kickDepleted

	private fun notifyRefresh(text: String) {
		Minecraft.getInstance().player?.sendSystemMessage(
			net.minecraft.network.chat.Component.literal(text)
		)
	}

	fun dropMissingNearPlayer(seenKeys: Set<SpotKey>, now: Long) {
		val client = Minecraft.getInstance()
		val player = client.player ?: return
		val level = client.level ?: return
		val toRemove = ArrayList<SpotKey>()
		for (spot in spots.values) {
			if (spot.key.dimension != level.dimension().identifier()) continue
			if (spot.key in seenKeys) continue
			val dx = (spot.x + 0.5) - player.x
			val dy = spot.y - player.y
			val dz = (spot.z + 0.5) - player.z
			val distSq = dx * dx + dy * dy + dz * dz
			if (distSq > depleteRangeSq) continue
			if (!level.isLoaded(net.minecraft.core.BlockPos(spot.x, spot.y, spot.z))) continue
			if (spot.pinned && !shouldKickDepleted()) {
				spot.stock = StockLevel.DEPLETED
				continue
			}
			toRemove.add(spot.key)
		}
		toRemove.forEach { remove(it) }
	}
}
