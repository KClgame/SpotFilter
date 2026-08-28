package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.parseHexColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class AutoPinRuleScreen(
	private val returnTo: Screen,
	private val rule: AutoPinRule
) : Screen(Component.literal("Auto Pin rule")) {
	private lateinit var nameBox: EditBox
	private lateinit var colorBox: EditBox

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		nameBox = EditBox(font, 8, 32, width - 16, 20, Component.literal("Name"))
		nameBox.setMaxLength(32)
		nameBox.value = rule.name
		nameBox.setResponder { rule.name = it }
		addRenderableWidget(nameBox)

		addRenderableWidget(
			Button.builder(Component.literal("Mode: ${rule.mode.name}")) { button ->
				rule.mode = if (rule.mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
				button.setMessage(Component.literal("Mode: ${rule.mode.name}"))
				SpotFilterConfig.save()
			}.bounds(8, 58, 110, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal(if (rule.enabled) "Enabled" else "Disabled")) { button ->
				rule.enabled = !rule.enabled
				button.setMessage(Component.literal(if (rule.enabled) "Enabled" else "Disabled"))
				SpotFilterConfig.save()
				AutoPin.applyAll()
			}.bounds(124, 58, 100, 20).build()
		)

		val slotWidth = ((width - 24) / 3).coerceAtLeast(90)
		repeat(3) { index ->
			addRenderableWidget(
				Button.builder(Component.literal("F${index + 1}: ${rule.slots[index].compactLabel()}")) { _ ->
					minecraft.gui.setScreen(FilterSlotScreen(this, rule.slots[index], "Auto Pin F${index + 1}"))
				}.bounds(8 + index * (slotWidth + 4), 84, slotWidth, 20).build()
			)
		}

		addRenderableWidget(
			Button.builder(Component.literal(rule.stock.compactLabel())) { _ ->
				minecraft.gui.setScreen(StockFilterScreen(this, rule.stock, applyAutoPin = true))
			}.bounds(8, 110, width - 16, 20).build()
		)

		colorBox = EditBox(font, 8, 148, 160, 20, Component.literal("Color"))
		colorBox.setMaxLength(7)
		colorBox.setHint(Component.literal("#RRGGBB"))
		colorBox.value = rule.customColorHex
		colorBox.setResponder {
			rule.customColorHex = it
			SpotFilterConfig.save()
			AutoPin.applyAll()
		}
		addRenderableWidget(colorBox)
		addRenderableWidget(
			Button.builder(Component.literal("Use default colors")) { _ ->
				rule.customColorHex = ""
				colorBox.value = ""
				SpotFilterConfig.save()
				AutoPin.applyAll()
			}.bounds(174, 148, 140, 20).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, Component.literal("Auto Pin rule"), 8, 10, 0xFFFFFFFF.toInt(), false)
		graphics.text(font, Component.literal("Pin color hex (empty = Strong/Wise/Pearl/Treasure/Spirit defaults)"), 8, 136, 0xFFAAAAAA.toInt(), false)
		val rgb = parseHexColor(rule.customColorHex)
		if (rgb != null) {
			graphics.fill(320, 148, 340, 168, 0xFF000000.toInt() or rgb)
		}
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose()
			return true
		}
		return super.keyPressed(event)
	}

	override fun onClose() {
		rule.name = nameBox.value.ifBlank { rule.name }
		rule.customColorHex = colorBox.value
		SpotFilterConfig.save()
		AutoPin.applyAll()
		minecraft.gui.setScreen(returnTo)
	}
}
