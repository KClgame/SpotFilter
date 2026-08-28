package kcl.spotfilter.client.ui

import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotKind
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

enum class HudLayout(val label: String) {
	DETAILED("Detailed"),
	COMPACT("Compact");

	fun toggle(): HudLayout = if (this == COMPACT) DETAILED else COMPACT

	companion object {
		fun fromName(raw: String?): HudLayout =
			entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: DETAILED
	}
}

data class LinePart(
	val text: Component? = null,
	val icon: Identifier? = null,
	val gapAfter: Int = 4
)

object SpotLines {
	const val ICON = 10
	const val COMPACT_ICON = 8
	private const val TEX = 16
	private const val WHITE = 0xFFFFFF

	fun lineH(font: Font): Int = maxOf(font.lineHeight, COMPACT_ICON)

	fun compactPerks(spot: FishingSpot) =
		spot.perks.filter { !it.type.isGrottoChance }.ifEmpty { spot.perks }.take(3)

	fun compactParts(spot: FishingSpot): List<LinePart> {
		val parts = ArrayList<LinePart>(12)
		val idRgb = if (spot.kind == SpotKind.GROTTO) spot.stabilityDisplayRgb() else WHITE
		parts += LinePart(
			text = Component.literal(spot.groupLabel()).withStyle(Style.EMPTY.withColor(idRgb)),
			gapAfter = 3
		)
		parts += LinePart(
			text = Component.literal("#${spot.rankNumber()}").withStyle(Style.EMPTY.withColor(idRgb)),
			gapAfter = 6
		)
		val perks = compactPerks(spot)
		for ((i, perk) in perks.withIndex()) {
			val valueColor = perk.valueRgb ?: WHITE
			parts += LinePart(
				text = Component.literal(perk.type.valueLabel(perk.value))
					.withStyle(Style.EMPTY.withColor(valueColor)),
				gapAfter = 1
			)
			parts += LinePart(
				icon = perk.type.textureId,
				gapAfter = if (i == perks.lastIndex) 6 else 5
			)
		}
		parts += LinePart(
			text = Component.literal("${spot.x} ${spot.y} ${spot.z}")
				.withStyle(Style.EMPTY.withColor(WHITE)),
			gapAfter = 5
		)
		parts += LinePart(
			text = Component.literal(spot.stock?.label ?: "?")
				.withStyle(Style.EMPTY.withColor(spot.stockDisplayRgb())),
			gapAfter = 0
		)
		return parts
	}

	fun width(font: Font, parts: List<LinePart>): Int {
		if (parts.isEmpty()) return 0
		var w = 0
		for (part in parts) {
			if (part.icon != null) w += COMPACT_ICON
			if (part.text != null) w += font.width(part.text)
			w += part.gapAfter
		}
		return w
	}

	fun draw(
		graphics: GuiGraphicsExtractor,
		font: Font,
		parts: List<LinePart>,
		x: Int,
		y: Int
	) {
		val lh = lineH(font)
		var cursor = x
		for (part in parts) {
			if (part.icon != null) {
				val iconY = y + (lh - COMPACT_ICON).coerceAtLeast(0) / 2
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					part.icon,
					cursor,
					iconY,
					0f,
					0f,
					COMPACT_ICON,
					COMPACT_ICON,
					TEX,
					TEX
				)
				cursor += COMPACT_ICON
			}
			if (part.text != null) {
				graphics.text(font, part.text, cursor, y, 0xFFFFFFFF.toInt(), false)
				cursor += font.width(part.text)
			}
			cursor += part.gapAfter
		}
	}
}
