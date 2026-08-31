package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterSlot
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class FilterSlotScreen(
	private val returnTo: Screen,
	private val slot: FilterSlot,
	private val heading: String
) : Screen(Component.literal(heading)) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		val perk = slot.perk
		addRenderableWidget(
			Button.builder(Component.literal("Perk: ${perk?.displayName ?: "None"}")) { _ ->
				minecraft.gui.setScreen(PerkPickerScreen(slot, heading, returnTo))
			}.bounds(width / 2 - 120, 40, 240, 20).build()
		)

		if (perk != null && perk.hasVariableValue) {
			addRenderableWidget(
				Button.builder(Component.literal("Compare: ${slot.compare.label} (${slot.compare.symbol})")) { _ ->
					slot.cycleCompare()
					SpotFilterConfig.save()
					rebuildWidgets()
				}.bounds(width / 2 - 120, 68, 240, 20).build()
			)
			if (slot.compare == CompareOp.BETWEEN) {
				addRenderableWidget(
					Button.builder(Component.literal("Lower: ${perk.valueLabel(slot.threshold)}")) { button ->
						slot.cycleThreshold()
						button.setMessage(Component.literal("Lower: ${perk.valueLabel(slot.threshold)}"))
						SpotFilterConfig.save()
					}.bounds(width / 2 - 120, 96, 116, 20).build()
				)
				addRenderableWidget(
					Button.builder(Component.literal("Upper: ${perk.valueLabel(slot.thresholdMax)}")) { button ->
						slot.cycleThresholdMax()
						button.setMessage(Component.literal("Upper: ${perk.valueLabel(slot.thresholdMax)}"))
						SpotFilterConfig.save()
					}.bounds(width / 2 + 4, 96, 116, 20).build()
				)
			} else {
				addRenderableWidget(
					Button.builder(Component.literal("Value: ${perk.valueLabel(slot.threshold)}")) { button ->
						slot.cycleThreshold()
						button.setMessage(Component.literal("Value: ${perk.valueLabel(slot.threshold)}"))
						SpotFilterConfig.save()
					}.bounds(width / 2 - 120, 96, 240, 20).build()
				)
			}
		}

		addRenderableWidget(
			Button.builder(Component.literal("Sort: ${slot.sortDir.label}")) { button ->
				slot.cycleSort()
				button.setMessage(Component.literal("Sort: ${slot.sortDir.label}"))
				SpotFilterConfig.save()
			}.bounds(width / 2 - 120, 124, 240, 20).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Clear this filter")) { _ ->
				slot.clear()
				SpotFilterConfig.save()
				rebuildWidgets()
			}.bounds(width / 2 - 120, 160, 240, 20).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, Component.literal(heading), width / 2 - 40, 16, 0xFFFFFFFF.toInt(), false)
		val perk = slot.perk
		if (perk != null && !perk.hasVariableValue) {
			graphics.text(
				font,
				Component.literal("Fixed value — selected means this perk is present"),
				width / 2 - 110,
				70,
				0xFFAAAAAA.toInt(),
				false
			)
		}
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
		SpotFilterConfig.save()
		minecraft.gui.setScreen(returnTo)
	}
}
