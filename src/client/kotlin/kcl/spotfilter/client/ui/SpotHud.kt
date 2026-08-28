package kcl.spotfilter.client.ui

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotKind
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
	private const val ICON = SpotLines.ICON
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

	private fun compact(): Boolean = SpotFilterConfig.instance.layout() == HudLayout.COMPACT

	private fun lineH(font: Font): Int = SpotLines.lineH(font)

	private fun headerWidth(font: Font, spot: FishingSpot): Int {
		val textW = font.width(header(spot))
		return if (spot.primaryPerk() != null) ICON + 4 + textW else textW
	}

	private fun blockSize(font: Font, pinned: List<FishingSpot>): Pair<Int, Int> {
		val lh = lineH(font)
		if (pinned.isEmpty()) {
			return font.width(EMPTY) to lh
		}
		val compact = compact()
		var blockW = 0
		var blockH = 0
		for ((i, spot) in pinned.withIndex()) {
			if (i > 0) blockH += 2
			if (compact) {
				blockW = maxOf(blockW, SpotLines.width(font, SpotLines.compactParts(spot)))
				blockH += lh
			} else {
				blockW = maxOf(blockW, headerWidth(font, spot))
				blockH += lh
				for (perk in spot.perks) {
					blockW = maxOf(blockW, font.width(perk.coloredLine()))
					blockH += lh
				}
			}
		}
		return blockW to blockH
	}

	private fun measure(font: Font, pinned: List<FishingSpot>): Pair<Int, Int> {
		val (blockW, blockH) = blockSize(font, pinned)
		return blockW + PAD * 2 to blockH + PAD * 2
	}

	private fun drawContent(graphics: GuiGraphicsExtractor, font: Font, w: Int, h: Int) {
		val alpha = (SpotFilterConfig.instance.backgroundAlpha / 100.0 * 180).toInt().coerceIn(0, 180)
		graphics.fill(0, 0, w, h, ARGB.color(alpha, 0, 0, 0))
		val pinned = SpotPool.pinned()
		val lh = lineH(font)
		val (blockW, blockH) = blockSize(font, pinned)
		val originX = (w - blockW) / 2
		var cursor = (h - blockH) / 2
		if (pinned.isEmpty()) {
			graphics.text(font, EMPTY, originX, cursor, GRAY, false)
			return
		}
		val compact = compact()
		for ((i, spot) in pinned.withIndex()) {
			if (i > 0) cursor += 2
			if (compact) {
				SpotLines.draw(graphics, font, SpotLines.compactParts(spot), originX, cursor)
				cursor += lh
				continue
			}
			val head = header(spot)
			val primary = spot.primaryPerk()
			if (primary != null) {
				val iconY = cursor + (lh - ICON).coerceAtLeast(0) / 2
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					primary.type.textureId,
					originX,
					iconY,
					0f,
					0f,
					ICON,
					ICON,
					ICON,
					ICON
				)
				graphics.text(font, head, originX + ICON + 4, cursor, WHITE, false)
			} else {
				graphics.text(font, head, originX, cursor, WHITE, false)
			}
			cursor += lh
			for (perk in spot.perks) {
				graphics.text(font, perk.coloredLine(), originX, cursor, WHITE, false)
				cursor += lh
			}
		}
	}

	private fun header(spot: FishingSpot): Component {
		val stock = spot.stock
		val stockText = Component.literal(stock?.label ?: "?").withStyle(
			Style.EMPTY.withColor(spot.stockDisplayRgb())
		)
		val text = Component.literal("${spot.displayTitle()}  ${spot.x} ${spot.y} ${spot.z}  ").append(stockText)
		val range = spot.stabilityRange
		if (spot.kind == SpotKind.GROTTO && !range.isNullOrBlank()) {
			text.append(Component.literal("  "))
			text.append(
				Component.literal(range).withStyle(
					Style.EMPTY.withColor(spot.stabilityDisplayRgb())
				)
			)
		}
		return text
	}
}
