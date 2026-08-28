package kcl.spotfilter.client.ui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object UiLayout {
	const val PAD = 8
	const val GAP = 4
	const val BTN = 20
	const val ROW = BTN + GAP

	fun split(screenWidth: Int, count: Int, index: Int, pad: Int = PAD, gap: Int = GAP): Pair<Int, Int> {
		val inner = screenWidth - pad * 2
		val base = (inner - gap * (count - 1)) / count
		val rem = inner - gap * (count - 1) - base * count
		val w = if (index == count - 1) base + rem else base
		val x = pad + index * (base + gap)
		return x to w
	}

	fun centerX(screenWidth: Int, widgetWidth: Int): Int = (screenWidth - widgetWidth) / 2

	fun textX(font: Font, text: Component, screenWidth: Int): Int =
		(screenWidth - font.width(text)) / 2

	fun drawCentered(
		graphics: GuiGraphicsExtractor,
		font: Font,
		text: Component,
		screenWidth: Int,
		y: Int,
		color: Int
	) {
		graphics.text(font, text, textX(font, text, screenWidth), y, color, false)
	}

	fun packTop(height: Int, packHeight: Int): Int =
		((height - packHeight) / 2).coerceAtLeast(PAD)
}

fun Screen.drawCentered(graphics: GuiGraphicsExtractor, text: Component, y: Int, color: Int) {
	UiLayout.drawCentered(graphics, font, text, width, y, color)
}
