package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.FilterState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class AutoPinListScreen(
	private val returnTo: Screen
) : Screen(Component.literal("Auto Pin")) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		var y = 36
		FilterState.autoPinRules.forEachIndexed { index, rule ->
			addRenderableWidget(
				Button.builder(Component.literal(if (rule.enabled) "On" else "Off")) { _ ->
					rule.enabled = !rule.enabled
					SpotFilterConfig.save()
					AutoPin.applyAll()
					rebuildWidgets()
				}.bounds(8, y, 40, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal(rule.name)) { _ ->
					minecraft.gui.setScreen(AutoPinRuleScreen(this, rule))
				}.bounds(52, y, width - 160, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Del")) { _ ->
					FilterState.autoPinRules.removeAt(index)
					SpotFilterConfig.save()
					AutoPin.applyAll()
					rebuildWidgets()
				}.bounds(width - 100, y, 92, 20).build()
			)
			y += 24
		}
		addRenderableWidget(
			Button.builder(Component.literal("Add rule")) { _ ->
				val rule = AutoPinRule()
				rule.name = "Rule ${FilterState.autoPinRules.size + 1}"
				FilterState.autoPinRules.add(rule)
				SpotFilterConfig.save()
				minecraft.gui.setScreen(AutoPinRuleScreen(this, rule))
			}.bounds(8, y.coerceAtMost(height - 56), 120, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(
			font,
			Component.literal("Auto Pin rules — matching spots are pinned. Empty hex uses default family colors."),
			8,
			12,
			0xFFFFFFFF.toInt(),
			false
		)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_O) {
			onClose()
			return true
		}
		return super.keyPressed(event)
	}

	override fun onClose() {
		SpotFilterConfig.save()
		AutoPin.applyAll()
		minecraft.gui.setScreen(returnTo)
	}
}
