package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.filter.PerkPairFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class PairFilterScreen(
	private val returnTo: Screen,
	private val pair: PerkPairFilter,
	private val applyAutoPin: Boolean = false
) : Screen(Component.literal("Pair filter")) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		addRenderableWidget(
			Button.builder(Component.literal(if (pair.enabled) "Pair filter: On" else "Pair filter: Off")) { button ->
				pair.enabled = !pair.enabled
				button.setMessage(Component.literal(if (pair.enabled) "Pair filter: On" else "Pair filter: Off"))
				persist()
			}.bounds(width / 2 - 120, 40, 240, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal("Compare: ${pair.compare.label} (${pair.compare.symbol})")) { _ ->
				pair.cycleCompare()
				persist()
				rebuildWidgets()
			}.bounds(width / 2 - 120, 68, 240, 20).build()
		)
		if (pair.compare == CompareOp.BETWEEN) {
			addRenderableWidget(
				Button.builder(Component.literal("Lower: +${pair.threshold}%")) { button ->
					pair.cycleThreshold()
					button.setMessage(Component.literal("Lower: +${pair.threshold}%"))
					persist()
				}.bounds(width / 2 - 120, 96, 116, 20).build()
			)
			addRenderableWidget(
				Button.builder(Component.literal("Upper: +${pair.thresholdMax}%")) { button ->
					pair.cycleThresholdMax()
					button.setMessage(Component.literal("Upper: +${pair.thresholdMax}%"))
					persist()
				}.bounds(width / 2 + 4, 96, 116, 20).build()
			)
		} else {
			addRenderableWidget(
				Button.builder(Component.literal("Value: +${pair.threshold}%")) { button ->
					pair.cycleThreshold()
					button.setMessage(Component.literal("Value: +${pair.threshold}%"))
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
		graphics.text(font, Component.literal("Perk pair sum"), width / 2 - 50, 16, 0xFFFFFFFF.toInt(), false)
		val grotto = FilterState.kind == SpotKind.GROTTO
		val lines = if (grotto) {
			listOf(
				"Grotto: same pairing on the extra bonuses (100% Chance ignored).",
				"Fish: Strong Hook + Wise Hook",
				"Pearl: Glimmering Hook + Pearl Magnet",
				"Treasure: Greedy Hook + Treasure Magnet",
				"Spirit: Lucky Hook + Spirit Magnet",
				"Missing half counts as +0%. Range +10% to +60%."
			)
		} else {
			listOf(
				"Normal island spots, by type:",
				"Fish: Strong Hook + Wise Hook",
				"Pearl: Glimmering Hook + Pearl Magnet",
				"Treasure: Greedy Hook + Treasure Magnet",
				"Spirit: Lucky Hook + Spirit Magnet",
				"Missing half counts as +0%. Range +10% to +60%."
			)
		}
		var y = 128
		for (line in lines) {
			graphics.text(font, Component.literal(line), width / 2 - 160, y, 0xFFAAAAAA.toInt(), false)
			y += 12
		}
	}

	private fun persist() {
		SpotFilterConfig.save()
		if (applyAutoPin) AutoPin.applyAll()
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_O) {
			onClose()
			return true
		}
		return super.keyPressed(event)
	}

	override fun onClose() {
		persist()
		minecraft.gui.setScreen(returnTo)
	}
}
