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
		val col = 240
		val x = UiLayout.centerX(width, col)
		val perk = slot.perk
		val between = perk != null && perk.hasVariableValue && slot.compare == CompareOp.BETWEEN
		val valueRow = perk != null && perk.hasVariableValue
		val rowCount = 1 + (if (valueRow) 2 else 0) + 3
		var y = UiLayout.packTop(height, UiLayout.ROW * (rowCount + 1)) + UiLayout.ROW

		fun add(label: Component, w: Int = col, ox: Int = 0, action: (Button) -> Unit) {
			addRenderableWidget(Button.builder(label, action).bounds(x + ox, y, w, UiLayout.BTN).build())
		}

		add(Component.literal("Perk: ${perk?.displayName ?: "None"}")) { _ ->
			minecraft.gui.setScreen(PerkPickerScreen(slot, heading, returnTo))
		}
		if (valueRow) {
			y += UiLayout.ROW
			add(Component.literal("Compare: ${slot.compare.label} (${slot.compare.symbol})")) { _ ->
				slot.cycleCompare()
				SpotFilterConfig.save()
				rebuildWidgets()
			}
			y += UiLayout.ROW
			if (between) {
				val half = (col - UiLayout.GAP) / 2
				add(Component.literal("Min: ${perk.valueLabel(slot.threshold)}"), half) { button ->
					slot.cycleThreshold()
					button.setMessage(Component.literal("Min: ${perk.valueLabel(slot.threshold)}"))
					SpotFilterConfig.save()
				}
				add(
					Component.literal("Max: ${perk.valueLabel(slot.thresholdMax)}"),
					col - half - UiLayout.GAP,
					half + UiLayout.GAP
				) { button ->
					slot.cycleThresholdMax()
					button.setMessage(Component.literal("Max: ${perk.valueLabel(slot.thresholdMax)}"))
					SpotFilterConfig.save()
				}
			} else {
				add(Component.literal("Value: ${perk.valueLabel(slot.threshold)}")) { button ->
					slot.cycleThreshold()
					button.setMessage(Component.literal("Value: ${perk.valueLabel(slot.threshold)}"))
					SpotFilterConfig.save()
				}
			}
		}
		y += UiLayout.ROW
		add(Component.literal("Sort: ${slot.sortDir.label}")) { button ->
			slot.cycleSort()
			button.setMessage(Component.literal("Sort: ${slot.sortDir.label}"))
			SpotFilterConfig.save()
		}
		y += UiLayout.ROW
		add(Component.literal("Clear this filter")) { _ ->
			slot.clear()
			SpotFilterConfig.save()
			rebuildWidgets()
		}
		y += UiLayout.ROW
		add(Component.literal("Back")) { _ -> onClose() }
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		val perk = slot.perk
		val valueRow = perk != null && perk.hasVariableValue
		val rowCount = 1 + (if (valueRow) 2 else 0) + 3
		val y = UiLayout.packTop(height, UiLayout.ROW * (rowCount + 1))
		drawCentered(graphics, Component.literal(heading), y + 4, 0xFFFFFFFF.toInt())
		if (perk != null && !perk.hasVariableValue) {
			drawCentered(
				graphics,
				Component.literal("Fixed bonus — value compare is hidden"),
				y + UiLayout.ROW + UiLayout.BTN + 6,
				0xFFAAAAAA.toInt()
			)
		}
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
		minecraft.gui.setScreen(returnTo)
	}
}
