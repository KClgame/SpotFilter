package kcl.spotfilter.client.world

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.Display
import net.minecraft.world.phys.Vec3

object PinnedSpotMarker {
	const val TAG = "spotfilter_marker"

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
				val y = spot.y + 0.35
				val z = spot.z + 0.5
				val dx = x - camera.pos.x
				val dy = y - camera.pos.y
				val dz = z - camera.pos.z
				val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
				val scale = (dist / 10.0).coerceIn(1.0, 14.0).toFloat() * 0.025f
				val label = Component.literal("fishing spot #${spot.id}")
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
		entity.entityTags().contains(TAG)

	fun spawnOrUpdate(spot: FishingSpot) {
		// Drawn in COLLECT_SUBMITS; no world entity (see-through nametags ignore leaves).
	}

	fun sync(spot: FishingSpot) {
		// no-op
	}

	fun remove(id: Int) {
		// no-op
	}

	fun removeAll() {
		// no-op
	}

	fun tick() {
		// no-op
	}
}
