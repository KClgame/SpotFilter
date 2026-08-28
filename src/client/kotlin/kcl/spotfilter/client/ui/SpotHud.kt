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

	private fun lineH(font: Font): Int = font.lineHeight

	private fun measure(font: Font, pinned: List<FishingSpot>): Pair<Int, Int> {
		val lh = lineH(font)
		if (pinned.isEmpty()) {
			return font.width(EMPTY) + PAD * 2 to (PAD * 2 + lh)
		}
		var maxW = 0
		var height = PAD
		for ((i, spot) in pinned.withIndex()) {
			if (i > 0) height += 2
			maxW = maxOf(maxW, ICON + 4 + font.width(header(spot)))
			height += lh
			for (perk in spot.perks) {
				maxW = maxOf(maxW, font.width(perk.coloredLine()))
				height += lh
			}
		}
		height += PAD
		return maxW + PAD * 2 to height
	}

	private fun drawContent(graphics: GuiGraphicsExtractor, font: Font, w: Int, h: Int) {
		val alpha = (SpotFilterConfig.instance.backgroundAlpha / 100.0 * 180).toInt().coerceIn(0, 180)
		graphics.fill(0, 0, w, h, ARGB.color(alpha, 0, 0, 0))
		val pinned = SpotPool.pinned()
		val lh = lineH(font)
		if (pinned.isEmpty()) {
			drawCentered(graphics, font, EMPTY, PAD, w, GRAY)
			return
		}
		var cursor = PAD
		for ((i, spot) in pinned.withIndex()) {
			if (i > 0) cursor += 2
			val head = header(spot)
			val groupW = ICON + 4 + font.width(head)
			val startX = (w - groupW) / 2
			val primary = spot.primaryPerk()
			if (primary != null) {
				val iconY = cursor + (lh - ICON).coerceAtLeast(0) / 2
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					primary.type.textureId,
					startX,
					iconY,
					0f,
					0f,
					ICON,
					ICON,
					ICON,
					ICON
				)
			}
			graphics.text(font, head, startX + ICON + 4, cursor, WHITE, false)
			cursor += lh
			for (perk in spot.perks) {
				drawCentered(graphics, font, perk.coloredLine(), cursor, w, WHITE)
				cursor += lh
			}
		}
	}

	private fun drawCentered(
		graphics: GuiGraphicsExtractor,
		font: Font,
		text: Component,
		y: Int,
		boxW: Int,
		color: Int
	) {
		val x = ((boxW - font.width(text)) / 2).coerceAtLeast(0)
		graphics.text(font, text, x, y, color, false)
	}

	private fun header(spot: FishingSpot): Component {
		val stock = spot.stock
		val stockText = Component.literal(stock?.label ?: "?").withStyle(
			Style.EMPTY.withColor(spot.stockDisplayRgb())
		)
		return Component.literal("#${spot.id}  ${spot.x} ${spot.y} ${spot.z}  ").append(stockText)
	}
}
