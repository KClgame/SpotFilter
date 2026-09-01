package kcl.spotfilter.client.ui

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3

object SpotGuideOverlay {
	fun register() {
		HudElementRegistry.addLast(SpotFilter.id("spot_guides"), SpotGuideOverlay::extract)
	}

	fun extract(graphics: GuiGraphicsExtractor, deltaTracker: net.minecraft.client.DeltaTracker) {
		graphics.nextStratum()
		val client = Minecraft.getInstance()
		if (!SpotFilterConfig.instance.enabled) return
		val level = client.level ?: return
		if (client.player == null) return
		if (client.gui.hud.isHidden()) return
		if (client.gui.screen() != null) return

		val camera = client.gameRenderer.mainCamera()
		val camPos = camera.position()
		val forward = camera.forwardVector()
		val width = graphics.guiWidth()
		val height = graphics.guiHeight()
		val font = client.font
		val zoom = LogicalZoomCompat.ndcXyScale()

		for (spot in SpotPool.pinned()) {
			if (spot.key.dimension != level.dimension().identifier()) continue
			val wx = PinnedSpotMarker.worldX(spot)
			val wy = PinnedSpotMarker.worldY(spot)
			val wz = PinnedSpotMarker.worldZ(spot)
			val dx = wx - camPos.x
			val dy = wy - camPos.y
			val dz = wz - camPos.z
			val depth = dx * forward.x() + dy * forward.y() + dz * forward.z()
			if (depth <= 0.05) continue
			val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
			if (dist > PinnedSpotMarker.MAX_DIST) continue
			val ndc = client.gameRenderer.projectPointToScreen(Vec3(wx, wy, wz))
			if (!ndc.x.isFinite() || !ndc.y.isFinite() || !ndc.z.isFinite()) continue
			val sx = ((ndc.x * zoom + 1.0) * 0.5 * width).toFloat()
			val sy = ((1.0 - ndc.y * zoom) * 0.5 * height).toFloat()
			if (sx < 0f || sy < 0f || sx > width || sy > height) continue

			val rgb = spot.markerRgb()
			val label = Component.literal(PinnedSpotMarker.distanceLabel(spot, dist))
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)))
			val scale = PinnedSpotMarker.displayScale(dist)
			val pose = graphics.pose()
			pose.pushMatrix()
			pose.translate(sx, sy)
			pose.scale(scale, scale)
			val textW = font.width(label)
			graphics.text(font, label, -textW / 2, -font.lineHeight - 2, ARGB.opaque(rgb), true)
			pose.popMatrix()
		}
	}
}
