package kcl.spotfilter.client.data

import kcl.spotfilter.client.audio.SpotSounds
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.minecraft.client.Minecraft

object SpotPool {
	private const val DEPLETE_RANGE = 48.0
	private val depleteRangeSq = DEPLETE_RANGE * DEPLETE_RANGE

	private val spots = LinkedHashMap<SpotKey, FishingSpot>()
	private var nextId = 1
	private var lastLocalHour: Int? = null

	fun all(): Collection<FishingSpot> = spots.values

	fun pinned(): List<FishingSpot> = spots.values.filter { it.pinned }

	fun get(key: SpotKey): FishingSpot? = spots[key]

	fun byId(id: Int): FishingSpot? = spots.values.firstOrNull { it.id == id }

	fun upsert(incoming: FishingSpot) {
		val existing = spots[incoming.key]
		if (existing == null) {
			incoming.id = nextId++
			spots[incoming.key] = incoming
			SpotSounds.playNewSpot()
		} else {
			existing.entityId = incoming.entityId
			existing.x = incoming.x
			existing.y = incoming.y
			existing.z = incoming.z
			existing.stock = incoming.stock
			existing.stockRgb = incoming.stockRgb
			existing.perks = incoming.perks
			existing.lastSeenGameTime = incoming.lastSeenGameTime
			if (existing.pinned) {
				PinnedSpotMarker.sync(existing)
			}
		}
	}

	fun setPinned(spot: FishingSpot, pinned: Boolean) {
		spot.pinned = pinned
		if (pinned) {
			PinnedSpotMarker.spawnOrUpdate(spot)
		} else {
			PinnedSpotMarker.remove(spot.id)
		}
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
			toRemove.add(spot.key)
		}
		toRemove.forEach { remove(it) }
	}
}
