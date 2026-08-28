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
		var y = UiLayout.PAD + UiLayout.ROW
		nameBox = EditBox(font, UiLayout.PAD, y, width - UiLayout.PAD * 2, UiLayout.BTN, Component.literal("Name"))
		nameBox.setMaxLength(32)
		nameBox.value = rule.name
		nameBox.setResponder { rule.name = it }
		addRenderableWidget(nameBox)

		y += UiLayout.ROW
		val (modeX, modeW) = UiLayout.split(width, 2, 0)
		val (enX, enW) = UiLayout.split(width, 2, 1)
		addRenderableWidget(
			Button.builder(Component.literal("Mode: ${rule.mode.name}")) { button ->
				rule.mode = if (rule.mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
				button.setMessage(Component.literal("Mode: ${rule.mode.name}"))
				SpotFilterConfig.save()
			}.bounds(modeX, y, modeW, UiLayout.BTN).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal(if (rule.enabled) "Enabled" else "Disabled")) { button ->
				rule.enabled = !rule.enabled
				button.setMessage(Component.literal(if (rule.enabled) "Enabled" else "Disabled"))
				SpotFilterConfig.save()
				AutoPin.applyAll()
			}.bounds(enX, y, enW, UiLayout.BTN).build()
		)

		y += UiLayout.ROW
		repeat(3) { index ->
			val (x, w) = UiLayout.split(width, 3, index)
			addRenderableWidget(
				Button.builder(Component.literal("F${index + 1}: ${rule.slots[index].compactLabel()}")) { _ ->
					minecraft.gui.setScreen(FilterSlotScreen(this, rule.slots[index], "Auto Pin F${index + 1}"))
				}.bounds(x, y, w, UiLayout.BTN).build()
			)
		}

		y += UiLayout.ROW
		addRenderableWidget(
			Button.builder(Component.literal(rule.stock.compactLabel())) { _ ->
				minecraft.gui.setScreen(StockFilterScreen(this, rule.stock, applyAutoPin = true))
			}.bounds(UiLayout.PAD, y, width - UiLayout.PAD * 2, UiLayout.BTN).build()
		)

		y += UiLayout.ROW * 2
		val swatch = 20
		val colorW = ((width - UiLayout.PAD * 2 - UiLayout.GAP * 2 - swatch) * 2) / 5
		colorBox = EditBox(font, UiLayout.PAD, y, colorW, UiLayout.BTN, Component.literal("Color"))
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
			}.bounds(
				UiLayout.PAD + colorW + UiLayout.GAP,
				y,
				width - UiLayout.PAD * 2 - colorW - UiLayout.GAP * 2 - swatch,
				UiLayout.BTN
			).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(UiLayout.PAD, height - UiLayout.PAD - UiLayout.BTN, width - UiLayout.PAD * 2, UiLayout.BTN)
				.build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		drawCentered(graphics, Component.literal("Auto Pin rule"), UiLayout.PAD + 4, 0xFFFFFFFF.toInt())
		drawCentered(
			graphics,
			Component.literal("Pin color hex (empty = family defaults)"),
			UiLayout.PAD + UiLayout.ROW * 6 + 4,
			0xFFAAAAAA.toInt()
		)
		val rgb = parseHexColor(rule.customColorHex)
		if (rgb != null) {
			val x = width - UiLayout.PAD - 20
			val y = UiLayout.PAD + UiLayout.ROW * 7
			graphics.fill(x, y, x + 20, y + 20, 0xFF000000.toInt() or rgb)
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
