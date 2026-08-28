package kcl.spotfilter.client.ui

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.FilterSlot
import kcl.spotfilter.client.parse.PerkType
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW

class PerkPickerScreen(
	private val slot: FilterSlot,
	private val heading: String,
	private val returnTo: Screen
) : Screen(Component.literal("Select perk")) {
	private var search = ""
	private var scroll = 0
	private lateinit var searchBox: EditBox

	private val listTop get() = UiLayout.PAD + UiLayout.ROW * 2
	private val listBottom get() = height - UiLayout.PAD - UiLayout.ROW
	private val rowHeight = 22

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		searchBox = EditBox(font, UiLayout.PAD, UiLayout.PAD + UiLayout.ROW, width - UiLayout.PAD * 2, UiLayout.BTN, Component.literal("Search"))
		searchBox.setMaxLength(40)
		searchBox.setHint(Component.literal("Search perks..."))
		searchBox.value = search
		searchBox.setResponder { value ->
			search = value
			scroll = 0
		}
		addRenderableWidget(searchBox)
		addRenderableWidget(
			Button.builder(Component.literal("Back")) { _ -> onClose() }
				.bounds(UiLayout.centerX(width, 100), height - UiLayout.PAD - UiLayout.BTN, 100, UiLayout.BTN).build()
		)
		setInitialFocus(searchBox)
	}

	private fun visiblePerks(): List<PerkType?> {
		val query = search.trim()
		val matches = PerkType.entries.filter { perk ->
			query.isEmpty() || perk.displayName.contains(query, ignoreCase = true)
		}
		return listOf<PerkType?>(null) + matches
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		drawCentered(graphics, Component.literal("Choose a perk"), UiLayout.PAD + 4, 0xFFFFFFFF.toInt())

		val items = visiblePerks()
		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		if (scroll > (items.size - visibleRows).coerceAtLeast(0)) {
			scroll = (items.size - visibleRows).coerceAtLeast(0)
		}
		val end = (scroll + visibleRows).coerceAtMost(items.size)
		var y = listTop
		for (i in scroll until end) {
			val perk = items[i]
			val hovered = mouseX in UiLayout.PAD until (width - UiLayout.PAD) && mouseY in y until (y + rowHeight - 2)
			val selected = slot.perk == perk
			val bg = when {
				selected && hovered -> 0x8866AA66.toInt()
				selected -> 0x55338855.toInt()
				hovered -> 0x55FFFFFF.toInt()
				else -> 0x33000000
			}
			graphics.fill(UiLayout.PAD, y, width - UiLayout.PAD, y + rowHeight - 2, bg)
			if (perk != null) {
				graphics.blit(
					RenderPipelines.GUI_TEXTURED,
					perk.textureId,
					UiLayout.PAD + 4,
					y + 2,
					0f,
					0f,
					16,
					16,
					16,
					16
				)
				graphics.text(
					font,
					Component.literal(perk.displayName).withStyle(perk.family.nameStyle()),
					UiLayout.PAD + 26,
					y + 5,
					0xFFFFFFFF.toInt(),
					false
				)
			} else {
				graphics.text(font, Component.literal("None").withStyle(Style.EMPTY.withColor(0xAAAAAA)), UiLayout.PAD + 4, y + 5, 0xFFAAAAAA.toInt(), false)
			}
			y += rowHeight
		}
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (event.button() == 0) {
			val items = visiblePerks()
			val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
			val my = event.y().toInt()
			val indexInList = scroll + ((my - listTop) / rowHeight)
			if (indexInList in items.indices && my in listTop until listBottom && indexInList < scroll + visibleRows) {
				slot.perk = items[indexInList]
				SpotFilterConfig.save()
				onClose()
				return true
			}
		}
		return super.mouseClicked(event, doubled)
	}

	override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
		val items = visiblePerks()
		val visibleRows = ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
		val max = (items.size - visibleRows).coerceAtLeast(0)
		scroll = (scroll - scrollY.toInt()).coerceIn(0, max)
		return true
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose()
			return true
		}
		return super.keyPressed(event)
	}

	override fun onClose() {
		minecraft.gui.setScreen(FilterSlotScreen(returnTo, slot, heading))
	}
}
