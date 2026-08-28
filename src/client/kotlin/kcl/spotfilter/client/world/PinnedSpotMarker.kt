package kcl.spotfilter.client.world

import com.mojang.math.Transformation
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.scan.TextDisplays
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntitySpawnRequest
import net.minecraft.world.entity.EntityTypes
import org.joml.Quaternionf
import org.joml.Vector3f

object PinnedSpotMarker {
	const val TAG = "spotfilter_marker"

	private val markers = HashMap<Int, Display.TextDisplay>()
	private var nextClientId = -1_000_000

	fun isOurs(entity: Display.TextDisplay): Boolean {
		if (entity.entityTags().contains(TAG)) return true
		if (markers.values.any { it === entity }) return true
		return false
	}

	fun spawnOrUpdate(spot: FishingSpot) {
		if (!SpotFilterConfig.instance.enabled) return
		val client = Minecraft.getInstance()
		val level = client.level ?: return
		if (level.dimension().identifier() != spot.key.dimension) return

		val existing = markers[spot.id]
		if (existing != null && existing.isAlive && existing.level() === level) {
			apply(existing, spot)
			return
		}
		remove(spot.id)
		val entity = EntityTypes.TEXT_DISPLAY.create(
			level,
			EntitySpawnRequest(EntitySpawnReason.LOAD, true)
		) ?: return
		entity.setId(nextClientId--)
		entity.addTag(TAG)
		entity.setPos(spot.x + 0.5, spot.y - 1.0, spot.z + 0.5)
		entity.setNoGravity(true)
		entity.setBillboardConstraints(Display.BillboardConstraints.CENTER)
		entity.setViewRange(8.0f)
		apply(entity, spot)
		level.addEntity(entity)
		markers[spot.id] = entity
	}

	fun sync(spot: FishingSpot) {
		if (spot.pinned) spawnOrUpdate(spot) else remove(spot.id)
	}

	fun remove(id: Int) {
		markers.remove(id)?.discard()
	}

	fun removeAll() {
		markers.values.forEach { it.discard() }
		markers.clear()
	}

	fun tick() {
		if (!SpotFilterConfig.instance.enabled) {
			removeAll()
			return
		}
		val client = Minecraft.getInstance()
		val level = client.level
		if (level == null) {
			removeAll()
			return
		}
		val pinned = SpotPool.pinned()
		val keep = pinned.map { it.id }.toHashSet()
		val stale = markers.keys.filter { it !in keep }
		stale.forEach { remove(it) }
		for (spot in pinned) {
			if (spot.key.dimension != level.dimension().identifier()) {
				remove(spot.id)
				continue
			}
			spawnOrUpdate(spot)
		}
	}

	private fun apply(entity: Display.TextDisplay, spot: FishingSpot) {
		entity.setPos(spot.x + 0.5, spot.y - 1.0, spot.z + 0.5)
		val label = Component.literal("fishing spot #${spot.id}")
			.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(spot.markerRgb())))
		TextDisplays.writeText(entity, label)
		TextDisplays.setSeeThrough(entity)
		val player = Minecraft.getInstance().player
		val dist = if (player == null) 16.0 else player.distanceTo(entity).toDouble()
		val scale = (dist / 10.0).coerceIn(1.0, 14.0).toFloat()
		entity.setTransformation(
			Transformation(
				Vector3f(),
				Quaternionf(),
				Vector3f(scale, scale, scale),
				Quaternionf()
			)
		)
	}
}
