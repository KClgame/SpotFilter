package kcl.spotfilter.client.data

import kcl.spotfilter.client.audio.SpotSounds
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.minecraft.client.Minecraft

object SpotPool {
	private const val DEPLETE_RANGE = 48.0
	private val depleteRangeSq = DEPLETE_RANGE * DEPLETE_RANGE

	private val spots = LinkedHashMap<SpotKey, FishingSpot>()
	private var nextId = 1
	private var lastLocalHour: Int? = null

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
		val existing = spots[incoming.key]
		if (existing == null) {
			incoming.id = nextId++
			spots[incoming.key] = incoming
			SpotSounds.playNewSpot()
			AutoPin.apply(incoming)
		} else {
			val becameDepleted =
				incoming.stock == StockLevel.DEPLETED && existing.stock != StockLevel.DEPLETED
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
			if (becameDepleted && existing.autoPinned) {
				setPinned(existing, false)
			}
			AutoPin.apply(existing)
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

	fun tickHourlyReset() {
		val hour = java.time.LocalTime.now().hour
		val previous = lastLocalHour
		lastLocalHour = hour
		if (previous != null && previous != hour) {
			clearSpots()
			val client = Minecraft.getInstance()
			client.player?.sendSystemMessage(
				net.minecraft.network.chat.Component.literal("SpotFilter: fishing spots refreshed")
			)
		}
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
			if (spot.pinned && !spot.autoPinned) {
				spot.stock = StockLevel.DEPLETED
				continue
			}
			toRemove.add(spot.key)
		}
		toRemove.forEach { remove(it) }
	}
}
