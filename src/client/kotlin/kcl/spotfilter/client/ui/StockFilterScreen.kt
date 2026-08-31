package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.StockFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class StockFilterScreen(
	private val returnTo: Screen,
	private val stock: StockFilter,
	private val applyAutoPin: Boolean = false
) : Screen(Component.literal("Stock filter")) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		addRenderableWidget(
			Button.builder(Component.literal(if (stock.enabled) "Stock filter: On" else "Stock filter: Off")) { button ->
				stock.enabled = !stock.enabled
				button.setMessage(Component.literal(if (stock.enabled) "Stock filter: On" else "Stock filter: Off"))
				persist()
			}.bounds(width / 2 - 120, 40, 240, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Compare: ${stock.compare.label} (${stock.compare.symbol})")) { _ ->
				stock.cycleCompare()
				persist()
				rebuildWidgets()
			}.bounds(width / 2 - 120, 68, 240, 20).build()
		)
		if (stock.compare == CompareOp.BETWEEN) {
			addRenderableWidget(
				Button.builder(Component.literal("Lower: ${stock.level.label}")) { button ->
					stock.cycleLevel()
					button.setMessage(Component.literal("Lower: ${stock.level.label}"))
					persist()
				}.bounds(width / 2 - 120, 96, 116, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Upper: ${stock.levelMax.label}")) { button ->
					stock.cycleLevelMax()
					button.setMessage(Component.literal("Upper: ${stock.levelMax.label}"))
					persist()
				}.bounds(width / 2 + 4, 96, 116, 20).build()
			)
		} else {
			addRenderableWidget(
				Button.builder(Component.literal("Level: ${stock.level.label}")) { button ->
					stock.cycleLevel()
					button.setMessage(Component.literal("Level: ${stock.level.label}"))
					persist()
				}.bounds(width / 2 - 120, 96, 240, 20).build()
			)
		}
		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, Component.literal("Stock filter"), width / 2 - 40, 16, 0xFFFFFFFF.toInt(), false)
		graphics.text(
			font,
			Component.literal("Plentiful > Very High > High > Medium > Low > Depleted"),
			width / 2 - 140,
			130,
			0xFFAAAAAA.toInt(),
			false
		)
	}

	private fun persist() {
		SpotFilterConfig.save()
		if (applyAutoPin) AutoPin.applyAll()
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose()
			return true
		}
		if (super.keyPressed(event)) return true
		if (typingInBox()) return true
		if (event.key() == GLFW.GLFW_KEY_O) {
			onClose()
			return true
		}
		return false
	}

	override fun onClose() {
		persist()
		minecraft.gui.setScreen(returnTo)
	}
}
