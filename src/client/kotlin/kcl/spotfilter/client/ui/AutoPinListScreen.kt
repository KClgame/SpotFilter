package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.RulePack
import kcl.spotfilter.client.config.RulePacks
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotKind
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
	private val returnTo: Screen,
	private val pack: RulePack
) : Screen(Component.literal("Auto Pin")) {
	private val rules get() = pack.rules(FilterState.kind)

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		addRenderableWidget(
			Button.builder(Component.literal("${FilterState.kind.label}  (${rules.size} rules)")) { _ ->
				FilterState.toggleKind()
				SpotFilterConfig.save()
				rebuildWidgets()
			}.bounds(width - 188, 8, 180, 20).build()
		)
		var y = 36
		rules.forEachIndexed { index, rule ->
			addRenderableWidget(
				Button.builder(Component.literal(if (rule.enabled) "On" else "Off")) { _ ->
					rule.enabled = !rule.enabled
					persist()
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
					rules.removeAt(index)
					persist()
					rebuildWidgets()
				}.bounds(width - 100, y, 92, 20).build()
			)
			y += 24
		}
		addRenderableWidget(
			Button.builder(Component.literal("Add rule")) { _ ->
				val rule = AutoPinRule()
				rule.name = "Rule ${rules.size + 1}"
				rules.add(rule)
				persist()
				minecraft.gui.setScreen(AutoPinRuleScreen(this, rule))
			}.bounds(8, y.coerceAtMost(height - 56), 120, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Reload pack")) { _ ->
				SpotFilterConfig.reload()
				AutoPin.applyAll()
				rebuildWidgets()
			}.bounds(136, y.coerceAtMost(height - 56), 140, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	private fun persist() {
		RulePacks.savePack(pack)
		RulePacks.syncToFilterState()
		SpotFilterConfig.save()
		AutoPin.applyAll()
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(
			font,
			Component.literal(
				"Pack '${pack.id}' (${FilterState.kind.label}) — ${
					if (FilterState.kind == SpotKind.GROTTO) "${pack.id}_grotto.txt" else "${pack.id}.txt"
				}  |  Duplicate rule names share # numbering."
			),
			8,
			12,
			0xFFFFFFFF.toInt(),
			false
		)
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
