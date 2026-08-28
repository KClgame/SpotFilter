package kcl.spotfilter.client.world

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.scan.TextDisplays
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3

object PinnedSpotMarker {
	const val TAG = "spotfilter_marker"

	private var nextClientId = -910_001
	private val entities = HashMap<Int, Display.TextDisplay>()

	fun register() {
		LevelRenderEvents.COLLECT_SUBMITS.register { context ->
			if (!SpotFilterConfig.instance.enabled) return@register
			val client = Minecraft.getInstance()
			if (client.level == null || client.player == null) return@register
			val camera = context.levelState().cameraRenderState
			val collector = context.submitNodeCollector()
			val pose = context.poseStack()
			for (spot in SpotPool.pinned()) {
				if (spot.key.dimension != client.level!!.dimension().identifier()) continue
				val x = spot.x + 0.5
				val y = spot.y - 1.0
				val z = spot.z + 0.5
				val dx = x - camera.pos.x
				val dy = y - camera.pos.y
				val dz = z - camera.pos.z
				val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
				val scale = (dist / 10.0).coerceIn(1.0, 14.0).toFloat() * 0.025f
				val label = Component.literal(spot.guideLabel())
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(spot.markerRgb())))
				pose.pushPose()
				pose.translate(dx, dy, dz)
				pose.scale(scale, scale, scale)
				collector.submitNameTag(
					pose,
					Vec3.ZERO,
					0,
					label,
					true,
					0xF000F0,
					camera
				)
				pose.popPose()
			}
		}
	}

	fun isOurs(entity: Display.TextDisplay): Boolean =
		entity.entityTags().contains(TAG) || entities.values.any { it === entity }

	fun spawnOrUpdate(spot: FishingSpot) {
		if (!SpotFilterConfig.instance.enabled) return
		val client = Minecraft.getInstance()
		val level = client.level ?: return
		if (spot.key.dimension != level.dimension().identifier()) return

		val label = Component.literal(spot.guideLabel())
			.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(spot.markerRgb())))
		val x = spot.x + 0.5
		val y = spot.y - 1.0
		val z = spot.z + 0.5

		val current = entities[spot.id]
		if (current != null && !current.isRemoved && current.level() === level) {
			current.setText(label)
			TextDisplays.setSeeThrough(current)
			current.snapTo(x, y, z)
			return
		}

		if (current != null) {
			discard(current)
			entities.remove(spot.id)
		}

		val entity = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level)
		entity.setId(nextClientId--)
		entity.addTag(TAG)
		entity.setBillboardConstraints(Display.BillboardConstraints.CENTER)
		entity.setViewRange(8.0f)
		entity.setText(label)
		TextDisplays.setSeeThrough(entity)
		entity.snapTo(x, y, z)
		level.addEntity(entity)
		entities[spot.id] = entity
	}

	fun sync(spot: FishingSpot) {
		if (spot.pinned) {
			spawnOrUpdate(spot)
		} else {
			remove(spot.id)
		}
	}

	fun remove(id: Int) {
		entities.remove(id)?.let { discard(it) }
	}

	fun removeAll() {
		entities.values.forEach { discard(it) }
		entities.clear()
	}

	fun tick() {
		val client = Minecraft.getInstance()
		val level = client.level
		if (level == null || !SpotFilterConfig.instance.enabled) {
			removeAll()
			return
		}
		val keep = HashSet<Int>()
		for (spot in SpotPool.pinned()) {
			if (spot.key.dimension != level.dimension().identifier()) continue
			keep.add(spot.id)
			spawnOrUpdate(spot)
		}
		entities.keys.filter { it !in keep }.toList().forEach { remove(it) }
	}

	private fun discard(entity: Display.TextDisplay) {
		if (entity.isRemoved) return
		val level = entity.level()
		if (level is net.minecraft.client.multiplayer.ClientLevel) {
			level.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
		} else {
			entity.discard()
		}
	}
}
