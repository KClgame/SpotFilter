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
		var y = UiLayout.PAD + UiLayout.ROW
		FilterState.autoPinRules.forEachIndexed { index, rule ->
			val (onX, onW) = UiLayout.split(width, 8, 0)
			addRenderableWidget(
				Button.builder(Component.literal(if (rule.enabled) "On" else "Off")) { _ ->
					rule.enabled = !rule.enabled
					SpotFilterConfig.save()
					AutoPin.applyAll()
					rebuildWidgets()
				}.bounds(onX, y, onW, UiLayout.BTN).build()
			)
			val nameX = UiLayout.split(width, 8, 1).first
			val del = UiLayout.split(width, 8, 7)
			addRenderableWidget(
				Button.builder(Component.literal(rule.name)) { _ ->
					minecraft.gui.setScreen(AutoPinRuleScreen(this, rule))
				}.bounds(nameX, y, del.first - nameX - UiLayout.GAP, UiLayout.BTN).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Del")) { _ ->
					FilterState.autoPinRules.removeAt(index)
					SpotFilterConfig.save()
					AutoPin.applyAll()
					rebuildWidgets()
				}.bounds(del.first, y, del.second, UiLayout.BTN).build()
			)
			y += UiLayout.ROW
		}
		val bottomY = height - UiLayout.PAD - UiLayout.BTN
		val (addX, addW) = UiLayout.split(width, 2, 0)
		val (backX, backW) = UiLayout.split(width, 2, 1)
		addRenderableWidget(
			Button.builder(Component.literal("Add rule")) { _ ->
				val rule = AutoPinRule()
				rule.name = "Rule ${FilterState.autoPinRules.size + 1}"
				FilterState.autoPinRules.add(rule)
				SpotFilterConfig.save()
				minecraft.gui.setScreen(AutoPinRuleScreen(this, rule))
			}.bounds(addX, bottomY, addW, UiLayout.BTN).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(backX, bottomY, backW, UiLayout.BTN).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		drawCentered(
			graphics,
			Component.literal("Auto Pin rules — matching spots are pinned"),
			UiLayout.PAD + 4,
			0xFFFFFFFF.toInt()
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
