package kcl.spotfilter.client.ui

import kcl.spotfilter.client.Hotkeys
import kcl.spotfilter.client.SpotFilterClient
import kcl.spotfilter.client.config.SpotFilterConfig
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class HotkeyScreen(private val returnTo: Screen) : Screen(Component.literal("SpotFilter Keys")) {
	private var recording: String? = null
	private val labelRows = ArrayList<Pair<Int, Component>>()

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		val cfg = SpotFilterConfig.instance
		labelRows.clear()
		var y = 40
		row(SpotFilterClient.openFilter, cfg.modOpenFilter, y)
		y += 24
		row(SpotFilterClient.clearSpots, cfg.modClearSpots, y)
		y += 24
		row(SpotFilterClient.toggleHud, cfg.modToggleHud, y)
		y += 24
		row(SpotFilterClient.highlightUp, cfg.modHighlightUp, y)
		y += 24
		row(SpotFilterClient.highlightDown, cfg.modHighlightDown, y)
		y += 24
		row(SpotFilterClient.lockHighlight, cfg.modLockHighlight, y)
		y += 24
		row(SpotFilterClient.clearHighlights, cfg.modClearHighlights, y)
		y += 24
		row(SpotFilterClient.toggleHighlight, cfg.modToggleHighlight, y)
		y += 24
		row(SpotFilterClient.toggleHighlightMode, cfg.modToggleHighlightMode, y)

		addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE) { _ ->
				SpotFilterConfig.save()
				minecraft.gui.setScreen(returnTo)
			}.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	private fun row(mapping: KeyMapping, modifier: Int, y: Int) {
		labelRows.add(y to Component.translatable(mapping.name))
		val bW = 130
		val aW = 96
		val bX = width - 12 - bW
		val aX = bX - 8 - aW
		val recordingThis = recording == mapping.name
		val modText = if (recordingThis) {
			Component.literal("> press key <")
		} else {
			Component.literal("A: ").append(Hotkeys.modifierLabel(modifier))
		}
		addRenderableWidget(
			Button.builder(modText) { _ ->
				recording = mapping.name
				rebuildWidgets()
			}.tooltip(
				Tooltip.create(
					Component.literal("Modifier A (default None). Click then press a key to record. Right-click sets None.")
				)
			).bounds(aX, y, aW, 20).build()
		)
		addRenderableWidget(
			Button.builder(
				Component.literal("B: ").append(mapping.translatedKeyMessage)
			) { _ -> }.tooltip(
				Tooltip.create(Component.literal("Main key B. Change it in Controls → SpotFilter."))
			).bounds(bX, y, bW, 20).build()
		)
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (event.button() == 1) {
			val cfg = SpotFilterConfig.instance
			val yHit = event.y().toInt()
			val index = ((yHit - 40) / 24)
			val setter: ((Int) -> Unit)? = when (index) {
				0 -> { v -> cfg.modOpenFilter = v }
				1 -> { v -> cfg.modClearSpots = v }
				2 -> { v -> cfg.modToggleHud = v }
				3 -> { v -> cfg.modHighlightUp = v }
				4 -> { v -> cfg.modHighlightDown = v }
				5 -> { v -> cfg.modLockHighlight = v }
				6 -> { v -> cfg.modClearHighlights = v }
				7 -> { v -> cfg.modToggleHighlight = v }
				8 -> { v -> cfg.modToggleHighlightMode = v }
				else -> null
			}
			val bW = 130
			val aW = 96
			val aX = width - 12 - bW - 8 - aW
			if (setter != null && event.x().toInt() in aX until (aX + aW)) {
				setter(Hotkeys.NONE)
				recording = null
				SpotFilterConfig.save()
				rebuildWidgets()
				return true
			}
		}
		return super.mouseClicked(event, doubled)
	}

	override fun keyPressed(event: KeyEvent): Boolean {
		val id = recording
		if (id != null) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				recording = null
				rebuildWidgets()
				return true
			}
			val cfg = SpotFilterConfig.instance
			when (id) {
				SpotFilterClient.openFilter.name -> cfg.modOpenFilter = event.key()
				SpotFilterClient.clearSpots.name -> cfg.modClearSpots = event.key()
				SpotFilterClient.toggleHud.name -> cfg.modToggleHud = event.key()
				SpotFilterClient.highlightUp.name -> cfg.modHighlightUp = event.key()
				SpotFilterClient.highlightDown.name -> cfg.modHighlightDown = event.key()
				SpotFilterClient.lockHighlight.name -> cfg.modLockHighlight = event.key()
				SpotFilterClient.clearHighlights.name -> cfg.modClearHighlights = event.key()
				SpotFilterClient.toggleHighlight.name -> cfg.modToggleHighlight = event.key()
				SpotFilterClient.toggleHighlightMode.name -> cfg.modToggleHighlightMode = event.key()
			}
			recording = null
			SpotFilterConfig.save()
			rebuildWidgets()
			return true
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			SpotFilterConfig.save()
			minecraft.gui.setScreen(returnTo)
			return true
		}
		return super.keyPressed(event)
	}

	override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		super.extractRenderState(graphics, mouseX, mouseY, delta)
		graphics.text(font, title, 12, 12, 0xFFFFFFFF.toInt(), false)
		for ((y, name) in labelRows) {
			graphics.text(font, name, 12, y + 6, 0xFFFFFFFF.toInt(), false)
		}
		graphics.text(
			font,
			Component.literal("A is the modifier (record here). B is the main key (Controls)."),
			12,
			height - 44,
			0xFFAAAAAA.toInt(),
			false
		)
	}

	override fun onClose() {
		SpotFilterConfig.save()
		minecraft.gui.setScreen(returnTo)
	}
}
