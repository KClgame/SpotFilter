package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotPool

import kcl.spotfilter.client.filter.FilterState
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW

class FilterScreen : Screen(Component.literal("SpotFilter")) {
	private var editingHud = false
	private var scroll = 0
	private var dragging = false
	private var dragOffX = 0
	private var dragOffY = 0

	private val listTop get() = 96
	private val listBottom get() = height - UiLayout.PAD
	private val rowHeight = 44

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		fillButtons()
	}

	private fun fillButtons() {
		if (editingHud) {
			addRenderableWidget(
				Button.builder(Component.literal("Done")) { _ ->
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
				}.bounds(width / 2 - 50, height - 28, 100, 20).build()
			)
			return
		}

		fun row(count: Int, index: Int, y: Int, label: Component, onClick: (Button) -> Unit) {
			val (x, w) = UiLayout.split(width, count, index)
			addRenderableWidget(Button.builder(label, onClick).bounds(x, y, w, UiLayout.BTN).build())
		}
		row(4, 0, UiLayout.PAD, modeLabel()) { _ ->
			FilterState.toggleMode()
			SpotFilterConfig.save()
			rebuildWidgets()
		}
		row(4, 1, UiLayout.PAD, Component.literal("Edit HUD")) { _ ->
			editingHud = true
			rebuildWidgets()
		}
		row(4, 2, UiLayout.PAD, Component.literal("Clear spots")) { _ ->
			SpotPool.clearSpots()
		}
		row(4, 3, UiLayout.PAD, enableLabel()) { _ ->
			val cfg = SpotFilterConfig.instance
			cfg.enabled = !cfg.enabled
			if (!cfg.enabled) {
				kcl.spotfilter.client.world.PinnedSpotMarker.removeAll()
			}
			SpotFilterConfig.save()
			rebuildWidgets()
		}

		repeat(3) { index ->
			row(3, index, UiLayout.PAD + UiLayout.ROW, slotLabel(index)) { _ ->
				minecraft.gui.setScreen(FilterSlotScreen(this, FilterState.slots[index], "Filter F${index + 1}"))
			}
		}
		row(2, 0, UiLayout.PAD + UiLayout.ROW * 2, Component.literal(FilterState.stock.compactLabel())) { _ ->
			minecraft.gui.setScreen(StockFilterScreen(this, FilterState.stock))
		}
		row(2, 1, UiLayout.PAD + UiLayout.ROW * 2, Component.literal("Auto Pin (${FilterState.autoPinRules.count { it.enabled }})")) { _ ->
			minecraft.gui.setScreen(AutoPinListScreen(this))
		}
	}

	private fun modeLabel(): Component =
		Component.literal("Mode: ${FilterState.mode.name}")

	private fun enableLabel(): Component =
		Component.literal(if (SpotFilterConfig.instance.enabled) "Enabled" else "Disabled")

	private fun slotLabel(index: Int): Component {
		val slot = FilterState.slots[index]
		return Component.literal("F${index + 1}: ${slot.compactLabel()}")
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		val font = this.font
		if (editingHud) {
			graphics.text(
				font,
				Component.literal(
					"Edit HUD: drag to move  |  scroll scale x${"%.1f".format(SpotFilterConfig.instance.hudScale)}  |  shift+scroll opacity ${SpotFilterConfig.instance.backgroundAlpha}%"
				),
				UiLayout.PAD,
				UiLayout.PAD,
				0xFFFFFFFF.toInt(),
				false
			)
			drawHudOutline(graphics)
			return
		}

		val spots = FilterState.filteredSorted()
		drawCentered(
			graphics,
			Component.literal("${spots.size} spots  |  click row to pin  |  F1>F2>F3 sort  |  O close"),
			UiLayout.PAD + UiLayout.ROW * 3,
			0xFFCCCCCC.toInt()
		)

		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		if (scroll > (spots.size - visibleRows).coerceAtLeast(0)) {
			scroll = (spots.size - visibleRows).coerceAtLeast(0)
		}
		val start = scroll
		val end = (scroll + visibleRows).coerceAtMost(spots.size)
		var y = listTop
		for (i in start until end) {
			val spot = spots[i]
			drawRow(graphics, spot, UiLayout.PAD, y, width - UiLayout.PAD * 2, mouseX, mouseY)
			y += rowHeight
		}
	}

	private fun drawHudOutline(graphics: GuiGraphicsExtractor) {
		val m = SpotHud.metrics()
		val x2 = m.x + m.screenWidth
		val y2 = m.y + m.screenHeight
		graphics.fill(m.x, m.y, x2, m.y + 1, 0xFFFFFFFF.toInt())
		graphics.fill(m.x, y2 - 1, x2, y2, 0xFFFFFFFF.toInt())
		graphics.fill(m.x, m.y, m.x + 1, y2, 0xFFFFFFFF.toInt())
		graphics.fill(x2 - 1, m.y, x2, y2, 0xFFFFFFFF.toInt())
	}

	private fun drawRow(
		graphics: GuiGraphicsExtractor,
		spot: kcl.spotfilter.client.data.FishingSpot,
		x: Int,
		y: Int,
		w: Int,
		mouseX: Int,
		mouseY: Int
	) {
		val hovered = mouseX in x until (x + w) && mouseY in y until (y + rowHeight - 2)
		val bg = when {
			spot.pinned && hovered -> 0x8866AA66.toInt()
			spot.pinned -> 0x55338855.toInt()
			hovered -> 0x55FFFFFF.toInt()
			else -> 0x33000000
		}
		graphics.fill(x, y, x + w, y + rowHeight - 2, bg)
		val primary = spot.primaryPerk()
		if (primary != null) {
			graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				primary.type.textureId,
				x + 4,
				y + 6,
				0f,
				0f,
				16,
				16,
				16,
				16
			)
		}
		val header = Component.literal("#${spot.id}  ${spot.x} ${spot.y} ${spot.z}  ")
			.append(
				Component.literal(spot.stock?.label ?: "?").withStyle(
					Style.EMPTY.withColor(spot.stockDisplayRgb())
				)
			)
			.append(Component.literal(if (spot.pinned) "  [PINNED]" else "  [pin]").withStyle(Style.EMPTY.withColor(if (spot.pinned) 0x88FF88 else 0x888888)))
		graphics.text(font, header, x + 26, y + 4, 0xFFFFFFFF.toInt(), false)
		var perkX = x + 26
		for (perk in spot.perks) {
			graphics.text(font, perk.coloredLine(), perkX, y + 18, 0xFFFFFFFF.toInt(), false)
			perkX += font.width(perk.coloredLine()) + 10
		}
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (editingHud) {
			val cfg = SpotFilterConfig.instance
			val mx = event.x().toInt()
			val my = event.y().toInt()
			if (inHud(mx, my)) {
				dragging = true
				dragOffX = mx - cfg.hudX
				dragOffY = my - cfg.hudY
				return true
			}
		} else if (event.button() == 0) {
			val spots = FilterState.filteredSorted()
			val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
			val index = scroll + ((event.y().toInt() - listTop) / rowHeight)
			if (index in spots.indices && event.y().toInt() in listTop until listBottom && index < scroll + visibleRows) {
				val spot = spots[index]
				SpotPool.setPinned(spot, !spot.pinned)
				return true
			}
		}
		return super.mouseClicked(event, doubled)
	}

	override fun mouseReleased(event: MouseButtonEvent): Boolean {
		if (dragging) {
			dragging = false
			SpotFilterConfig.save()
			return true
		}
		return super.mouseReleased(event)
	}

	override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
		val cfg = SpotFilterConfig.instance
		val mx = event.x().toInt()
		val my = event.y().toInt()
		if (dragging) {
			cfg.hudX = (mx - dragOffX).coerceAtLeast(0)
			cfg.hudY = (my - dragOffY).coerceAtLeast(0)
			return true
		}
		return super.mouseDragged(event, dx, dy)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		if (editingHud) {
			val cfg = SpotFilterConfig.instance
			if (minecraft.hasShiftDown()) {
				cfg.backgroundAlpha = (cfg.backgroundAlpha + if (scrollY > 0) 5 else -5).coerceIn(0, 90)
			} else {
				val step = if (scrollY > 0) 0.1f else -0.1f
				cfg.hudScale = ((cfg.hudScale + step) * 10f).toInt() / 10f
				cfg.clamp()
			}
			return true
		}
		val spots = FilterState.filteredSorted()
		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		val max = (spots.size - visibleRows).coerceAtLeast(0)
		scroll = (scroll - scrollY.toInt()).coerceIn(0, max)
		return true
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		when (event.key()) {
			GLFW.GLFW_KEY_E -> {
				if (!editingHud) {
					editingHud = true
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (editingHud) {
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_ESCAPE -> {
				if (editingHud) {
					editingHud = false
					SpotFilterConfig.save()
					rebuildWidgets()
					return true
				}
			}
			GLFW.GLFW_KEY_O -> {
				onClose()
				return true
			}
			GLFW.GLFW_KEY_P -> {
				SpotPool.clearSpots()
				return true
			}
		}
		return super.keyPressed(event)
	}

	private fun inHud(mx: Int, my: Int): Boolean {
		val m = SpotHud.metrics()
		return mx in m.x until (m.x + m.screenWidth) && my in m.y until (m.y + m.screenHeight)
	}

}
