package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterState
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
	private lateinit var nicknameBox: EditBox
	private lateinit var colorBox: EditBox
	private val grotto: Boolean get() = FilterState.kind == SpotKind.GROTTO

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		nameBox = EditBox(font, 8, 32, width - 16, 20, Component.literal("Name"))
		nameBox.setMaxLength(32)
		nameBox.value = rule.name
		nameBox.setResponder { rule.name = it }
		addRenderableWidget(nameBox)

		nicknameBox = EditBox(font, 8, 68, width - 16, 20, Component.literal("Nickname"))
		nicknameBox.setMaxLength(24)
		nicknameBox.setHint(Component.literal("Spot nickname (optional)"))
		nicknameBox.value = rule.nickname
		nicknameBox.setResponder {
			rule.nickname = it
			SpotFilterConfig.save()
			AutoPin.applyAll()
		}
		addRenderableWidget(nicknameBox)

		addRenderableWidget(
			Button.builder(Component.literal("Mode: ${rule.mode.name}")) { button ->
				rule.mode = if (rule.mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
				button.setMessage(Component.literal("Mode: ${rule.mode.name}"))
				SpotFilterConfig.save()
			}.bounds(8, 94, 110, 20).build()
		)
		addRenderableWidget(
			Button.builder(Component.literal(if (rule.enabled) "Enabled" else "Disabled")) { button ->
				rule.enabled = !rule.enabled
				button.setMessage(Component.literal(if (rule.enabled) "Enabled" else "Disabled"))
				SpotFilterConfig.save()
				AutoPin.applyAll()
			}.bounds(124, 94, 100, 20).build()
		)

		val slotWidth = ((width - 24) / 3).coerceAtLeast(90)
		repeat(3) { index ->
			addRenderableWidget(
				Button.builder(Component.literal("F${index + 1}: ${rule.slots[index].compactLabel()}")) { _ ->
					minecraft.gui.setScreen(FilterSlotScreen(this, rule.slots[index], "Auto Pin F${index + 1}"))
				}.bounds(8 + index * (slotWidth + 4), 120, slotWidth, 20).build()
			)
		}

		addRenderableWidget(
			Button.builder(Component.literal(rule.stock.compactLabel())) { _ ->
				minecraft.gui.setScreen(StockFilterScreen(this, rule.stock, applyAutoPin = true))
			}.bounds(8, 146, width - 16, 20).build()
		)

		var colorY = 186
		if (grotto) {
			addRenderableWidget(
				Button.builder(Component.literal(rule.stability.compactLabel())) { _ ->
					minecraft.gui.setScreen(StabilityFilterScreen(this, rule.stability, applyAutoPin = true))
				}.bounds(8, 172, width - 16, 20).build()
			)
			colorY = 212
		}

		colorBox = EditBox(font, 8, colorY, 160, 20, Component.literal("Color"))
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
			}.bounds(174, colorY, 140, 20).build()
		)

		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, Component.literal("Auto Pin rule"), 8, 10, 0xFFFFFFFF.toInt(), false)
		graphics.text(font, Component.literal("Nickname used on HUD and world guide as Name #1, #2… within this name"), 8, 56, 0xFFAAAAAA.toInt(), false)
		val colorHintY = if (grotto) 200 else 174
		graphics.text(
			font,
			Component.literal(
				if (grotto) {
					"Pin color hex (empty = highest bonus, or Chance color)"
				} else {
					"Pin color hex (empty = Strong/Wise/Pearl/Treasure/Spirit defaults)"
				}
			),
			8,
			colorHintY,
			0xFFAAAAAA.toInt(),
			false
		)
		val rgb = parseHexColor(rule.customColorHex)
		if (rgb != null) {
			val swatchY = if (grotto) 212 else 186
			graphics.fill(320, swatchY, 340, swatchY + 20, 0xFF000000.toInt() or rgb)
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
		rule.nickname = nicknameBox.value.trim()
		rule.customColorHex = colorBox.value
		SpotFilterConfig.save()
		AutoPin.applyAll()
		minecraft.gui.setScreen(returnTo)
	}
}
