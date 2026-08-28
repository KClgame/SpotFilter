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
	val icon: Identifier? = null
)

object SpotLines {
	const val ICON = 10
	private const val GAP = 4
	private const val WHITE = 0xFFFFFF

	fun lineH(font: Font): Int = maxOf(font.lineHeight, ICON)

	fun compactPerks(spot: FishingSpot) =
		spot.perks.filter { !it.type.isGrottoChance }.ifEmpty { spot.perks }.take(3)

	fun compactParts(spot: FishingSpot): List<LinePart> {
		val parts = ArrayList<LinePart>(10)
		val idRgb = if (spot.kind == SpotKind.GROTTO) spot.stabilityDisplayRgb() else WHITE
		val name = spot.nickname?.trim().orEmpty()
		if (name.isNotEmpty()) {
			parts += LinePart(text = Component.literal(name).withStyle(Style.EMPTY.withColor(idRgb)))
		}
		val number = if (name.isNotEmpty() && spot.groupIndex > 0) spot.groupIndex else spot.id
		parts += LinePart(text = Component.literal("#$number").withStyle(Style.EMPTY.withColor(idRgb)))
		for (perk in compactPerks(spot)) {
			val valueColor = perk.valueRgb ?: WHITE
			parts += LinePart(
				text = Component.literal(perk.type.valueLabel(perk.value))
					.withStyle(Style.EMPTY.withColor(valueColor))
			)
			parts += LinePart(icon = perk.type.textureId)
		}
		parts += LinePart(
			text = Component.literal("${spot.x} ${spot.y} ${spot.z}")
				.withStyle(Style.EMPTY.withColor(WHITE))
		)
		parts += LinePart(
			text = Component.literal(spot.stock?.label ?: "?")
				.withStyle(Style.EMPTY.withColor(spot.stockDisplayRgb()))
		)
		return parts
	}

	fun width(font: Font, parts: List<LinePart>): Int {
		if (parts.isEmpty()) return 0
		var w = 0
		for ((i, part) in parts.withIndex()) {
			if (i > 0) w += GAP
			if (part.icon != null) w += ICON
			if (part.text != null) w += font.width(part.text)
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
		for ((i, part) in parts.withIndex()) {
			if (i > 0) cursor += GAP
			if (part.icon != null) {
				val iconY = y + (lh - ICON).coerceAtLeast(0) / 2
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					part.icon,
					cursor,
					iconY,
					0f,
					0f,
					ICON,
					ICON,
					ICON,
					ICON
				)
				cursor += ICON
			}
			if (part.text != null) {
				graphics.text(font, part.text, cursor, y, 0xFFFFFFFF.toInt(), false)
				cursor += font.width(part.text)
			}
		}
	}
}
