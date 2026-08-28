package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.StockFilter
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class StockFilterScreen(
	private val returnTo: Screen,
	private val stock: StockFilter,
	private val applyAutoPin: Boolean = false
) : Screen(Component.literal("Stock filter")) {
	override fun isPauseScreen(): Boolean = false

	override fun init() {
		val col = 240
		val x = UiLayout.centerX(width, col)
		val extra = if (stock.compare == CompareOp.BETWEEN) 1 else 0
		val packH = UiLayout.ROW * (5 + extra)
		var y = UiLayout.packTop(height, packH) + UiLayout.ROW

		fun add(label: Component, w: Int = col, ox: Int = 0, action: (Button) -> Unit) {
			addRenderableWidget(Button.builder(label, action).bounds(x + ox, y, w, UiLayout.BTN).build())
		}

		add(Component.literal(if (stock.enabled) "Stock filter: On" else "Stock filter: Off")) { button ->
			stock.enabled = !stock.enabled
			button.setMessage(Component.literal(if (stock.enabled) "Stock filter: On" else "Stock filter: Off"))
			persist()
		}
		y += UiLayout.ROW
		add(Component.literal("Compare: ${stock.compare.label} (${stock.compare.symbol})")) { _ ->
			stock.cycleCompare()
			persist()
			rebuildWidgets()
		}
		y += UiLayout.ROW
		if (stock.compare == CompareOp.BETWEEN) {
			val half = (col - UiLayout.GAP) / 2
			add(Component.literal("From: ${stock.level.label}"), half, 0) { button ->
				stock.cycleLevel()
				button.setMessage(Component.literal("From: ${stock.level.label}"))
				persist()
			}
			add(Component.literal("To: ${stock.levelMax.label}"), col - half - UiLayout.GAP, half + UiLayout.GAP) { button ->
				stock.cycleLevelMax()
				button.setMessage(Component.literal("To: ${stock.levelMax.label}"))
				persist()
			}
		} else {
			add(Component.literal("Level: ${stock.level.label}")) { button ->
				stock.cycleLevel()
				button.setMessage(Component.literal("Level: ${stock.level.label}"))
				persist()
			}
		}
		y += UiLayout.ROW
		add(Component.literal("Back")) { _ -> onClose() }
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		val extra = if (stock.compare == CompareOp.BETWEEN) 1 else 0
		val packH = UiLayout.ROW * (5 + extra)
		val y = UiLayout.packTop(height, packH)
		drawCentered(graphics, Component.literal("Stock filter"), y + 4, 0xFFFFFFFF.toInt())
		drawCentered(
			graphics,
			Component.literal("Plentiful > Very High > High > Medium > Low > Depleted"),
			y + packH - 2,
			0xFFAAAAAA.toInt()
		)
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
