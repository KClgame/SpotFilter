package kcl.spotfilter.client.world

import kcl.spotfilter.client.data.FishingSpot
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.AABB

object PinnedSpotMarker {
	const val TAG = "spotfilter_marker"
	const val MAX_DIST = 512.0
	private const val CLOSE_SCALE = 1.0f
	private const val FAR_SCALE = 1.35f
	private const val CLOSE_DIST = 8.0
	private const val FAR_DIST = 80.0

	fun register() {
	}

	fun isOurs(entity: Display.TextDisplay): Boolean =
		entity.entityTags().contains(TAG)

	fun worldX(spot: FishingSpot): Double = spot.x + 0.5
	fun worldY(spot: FishingSpot): Double = spot.y - 0.5
	fun worldZ(spot: FishingSpot): Double = spot.z + 0.5

	fun displayScale(dist: Double): Float {
		val t = ((dist - CLOSE_DIST) / (FAR_DIST - CLOSE_DIST)).coerceIn(0.0, 1.0)
		return (CLOSE_SCALE + (FAR_SCALE - CLOSE_SCALE) * t).toFloat()
	}

	fun distanceLabel(spot: FishingSpot, dist: Double): String =
		"${spot.guideLabel()} ${dist.toInt()}m"

	fun spawnOrUpdate(spot: FishingSpot) {
	}

	fun sync(spot: FishingSpot) {
	}

	fun remove(id: Int) {
	}

	fun removeAll() {
		purgeLeftoverEntities()
	}

	fun tick() {
		purgeLeftoverEntities()
	}

	private fun purgeLeftoverEntities() {
		val client = Minecraft.getInstance()
		val level = client.level ?: return
		val player = client.player ?: return
		val box = AABB(player.position(), player.position()).inflate(MAX_DIST)
		for (entity in level.getEntities(EntityTypes.TEXT_DISPLAY, box) { isOurs(it) }) {
			discard(entity)
		}
	}

	private fun discard(entity: Display.TextDisplay) {
		if (entity.isRemoved) return
		val level = entity.level()
		if (level is ClientLevel) {
			try {
				level.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
			} catch (_: Exception) {
				entity.discard()
			}
		} else {
			entity.discard()
		}
	}
}
