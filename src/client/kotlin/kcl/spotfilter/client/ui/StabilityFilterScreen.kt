package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.StabilityFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class StabilityFilterScreen(
	private val returnTo: Screen,
	private val stability: StabilityFilter,
	private val applyAutoPin: Boolean = false
) : Screen(Component.literal("Stability Cost")) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		addRenderableWidget(
			Button.builder(Component.literal(if (stability.enabled) "Cost filter: On" else "Cost filter: Off")) { button ->
				stability.enabled = !stability.enabled
				button.setMessage(Component.literal(if (stability.enabled) "Cost filter: On" else "Cost filter: Off"))
				persist()
			}.bounds(width / 2 - 120, 40, 240, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Compare: ${stability.compare.label} (${stability.compare.symbol})")) { _ ->
				stability.cycleCompare()
				persist()
				rebuildWidgets()
			}.bounds(width / 2 - 120, 68, 240, 20).build()
		)
		if (stability.compare == CompareOp.BETWEEN) {
			addRenderableWidget(
				Button.builder(Component.literal("Lower: ${stability.level.label}")) { button ->
					stability.cycleLevel()
					button.setMessage(Component.literal("Lower: ${stability.level.label}"))
					persist()
				}.bounds(width / 2 - 120, 96, 116, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Upper: ${stability.levelMax.label}")) { button ->
					stability.cycleLevelMax()
					button.setMessage(Component.literal("Upper: ${stability.levelMax.label}"))
					persist()
				}.bounds(width / 2 + 4, 96, 116, 20).build()
			)
		} else {
			addRenderableWidget(
				Button.builder(Component.literal("Level: ${stability.level.label}")) { button ->
					stability.cycleLevel()
					button.setMessage(Component.literal("Level: ${stability.level.label}"))
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
		graphics.text(font, Component.literal("Stability Cost"), width / 2 - 50, 16, 0xFFFFFFFF.toInt(), false)
		graphics.text(
			font,
			Component.literal("Low (best)  >  Medium  >  High (worst)"),
			width / 2 - 110,
			130,
			0xFFAAAAAA.toInt(),
			false
		)
		graphics.text(
			font,
			Component.literal("Low #65FEFE   Medium #55FE56   High #FEFE55"),
			width / 2 - 130,
			146,
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
