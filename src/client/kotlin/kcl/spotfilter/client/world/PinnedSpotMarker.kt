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
	private const val CLOSE_SCALE = 2.0f
	private const val FAR_SCALE = 7.0f
	private const val CLOSE_DIST = 8.0
	private const val FAR_DIST = 56.0

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
				val scale = 0.022f * (displayScale(dist) / CLOSE_SCALE)
				val label = Component.literal(distanceLabel(spot, dist))
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
		// Nametag pipeline (seeThrough) is drawn in COLLECT_SUBMITS so cutout leaves cannot hide it.
	}

	fun sync(spot: FishingSpot) {}

	fun remove(id: Int) {}

	fun removeAll() {}

	fun tick() {}

	private fun displayScale(dist: Double): Float {
		val t = ((dist - CLOSE_DIST) / (FAR_DIST - CLOSE_DIST)).coerceIn(0.0, 1.0)
		return (CLOSE_SCALE + (FAR_SCALE - CLOSE_SCALE) * t).toFloat()
	}

	private fun distanceLabel(spot: FishingSpot, dist: Double): String =
		"${spot.guideLabel()} ${dist.toInt()}m"
}
