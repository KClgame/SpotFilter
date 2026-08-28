package kcl.spotfilter.client.ui

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB

data class HudMetrics(
	val x: Int,
	val y: Int,
	val contentWidth: Int,
	val contentHeight: Int,
	val scale: Float
) {
	val screenWidth: Int get() = (contentWidth * scale).toInt().coerceAtLeast(1)
	val screenHeight: Int get() = (contentHeight * scale).toInt().coerceAtLeast(1)
}

object SpotHud {
	private const val WHITE = 0xFFFFFFFF.toInt()
	private const val GRAY = 0xFFAAAAAA.toInt()
	private const val PAD = 6
	private const val LINE = 12
	private const val ICON = 10
	private val EMPTY = Component.literal("(No current fishing spot)").withStyle(Style.EMPTY.withColor(0xAAAAAA))

	fun register() {
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			SpotFilter.id("spot_coordinates"),
			SpotHud::extract
		)
	}

	fun metrics(): HudMetrics {
		val client = Minecraft.getInstance()
		val cfg = SpotFilterConfig.instance
		cfg.clamp()
		val (w, h) = measure(client.font, SpotPool.pinned())
		return HudMetrics(cfg.hudX, cfg.hudY, w, h, cfg.hudScale)
	}

	fun extract(graphics: GuiGraphicsExtractor, deltaTracker: net.minecraft.client.DeltaTracker) {
		val client = Minecraft.getInstance()
		if (client.level == null || client.player == null) return
		val cfg = SpotFilterConfig.instance
		if (!cfg.enabled) return
		if (!cfg.hudVisible && client.gui.screen() !is FilterScreen) return
		val metrics = metrics()
		val pose = graphics.pose()
		pose.pushMatrix()
		pose.translate(metrics.x.toFloat(), metrics.y.toFloat())
		pose.scale(metrics.scale, metrics.scale)
		drawContent(graphics, client.font, metrics.contentWidth, metrics.contentHeight)
		pose.popMatrix()
	}

	private fun measure(font: Font, pinned: List<FishingSpot>): Pair<Int, Int> {
		if (pinned.isEmpty()) {
			return font.width(EMPTY) + PAD * 2 to (PAD * 2 + LINE)
		}
		var maxW = 0
		var height = PAD
		for (spot in pinned) {
			maxW = maxOf(maxW, ICON + 4 + font.width(header(spot)))
			height += LINE
			for (perk in spot.perks) {
				maxW = maxOf(maxW, ICON + 4 + font.width(perk.coloredLine()))
				height += LINE
			}
			height += 2
		}
		height += PAD
		return maxW + PAD * 2 to height
	}

	private fun drawContent(graphics: GuiGraphicsExtractor, font: Font, w: Int, h: Int) {
		val alpha = (SpotFilterConfig.instance.backgroundAlpha / 100.0 * 180).toInt().coerceIn(0, 180)
		graphics.fill(0, 0, w, h, ARGB.color(alpha, 0, 0, 0))
		val pinned = SpotPool.pinned()
		if (pinned.isEmpty()) {
			graphics.text(font, EMPTY, PAD, PAD, GRAY, false)
			return
		}
		var cursor = PAD
		for (spot in pinned) {
			val primary = spot.primaryPerk()
			if (primary != null) {
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					primary.type.textureId,
					PAD,
					cursor,
					0f,
					0f,
					ICON,
					ICON,
					ICON,
					ICON
				)
			}
			graphics.text(font, header(spot), PAD + ICON + 4, cursor, WHITE, false)
			cursor += LINE
			for (perk in spot.perks) {
				graphics.text(font, perk.coloredLine(), PAD + ICON + 4, cursor, WHITE, false)
				cursor += LINE
			}
			cursor += 2
		}
	}

	private fun header(spot: FishingSpot): Component {
		val stock = spot.stock
		val stockText = Component.literal(stock?.label ?: "?").withStyle(
			Style.EMPTY.withColor(spot.stockDisplayRgb())
		)
		return Component.literal("#${spot.id}  ${spot.x} ${spot.y} ${spot.z}  ").append(stockText)
	}
}
