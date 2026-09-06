package kcl.spotfilter.client.ui

import kcl.spotfilter.client.Hotkeys
import kcl.spotfilter.client.SpotFilterClient
import kcl.spotfilter.client.config.SpotFilterConfig
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

	override fun isPauseScreen(): Boolean = false

	override fun init() {
		val cfg = SpotFilterConfig.instance
		addRenderableWidget(
			Button.builder(Component.literal("Highlight: ${cfg.highlightMode().label}")) { _ ->
				cfg.setHighlightMode(cfg.highlightMode().toggle())
				SpotFilterConfig.save()
				kcl.spotfilter.client.world.GlowPigs.removeAll()
				rebuildWidgets()
			}.tooltip(
				Tooltip.create(
					Component.literal("Glowing: invisible glowing pig at highlighted spots. Solo: HUD and guides show only highlighted spots.")
				)
			).bounds(12, 36, 240, 20).build()
		)
		var y = 64
		row("Open Filter", SpotFilterClient.openFilter, cfg.modOpenFilter, y)
		y += 24
		row("Clear spots", SpotFilterClient.clearSpots, cfg.modClearSpots, y)
		y += 24
		row("Toggle HUD", SpotFilterClient.toggleHud, cfg.modToggleHud, y)
		y += 24
		row("Highlight up", SpotFilterClient.highlightUp, cfg.modHighlightUp, y)
		y += 24
		row("Highlight down", SpotFilterClient.highlightDown, cfg.modHighlightDown, y)
		y += 24
		row("Lock highlight", SpotFilterClient.lockHighlight, cfg.modLockHighlight, y)
		y += 24
		row("Clear highlights", SpotFilterClient.clearHighlights, cfg.modClearHighlights, y)

		addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE) { _ ->
				SpotFilterConfig.save()
				minecraft.gui.setScreen(returnTo)
			}.bounds(width / 2 - 50, height - 28, 100, 20).build()
		)
	}

	private fun row(
		label: String,
		mapping: net.minecraft.client.KeyMapping,
		modifier: Int,
		y: Int
	) {
		val left = 12
		val modId = label
		val recordingThis = recording == modId
		val modText = if (recordingThis) {
			Component.literal("> press key <")
		} else {
			Component.literal("A: ").append(Hotkeys.modifierLabel(modifier))
		}
		addRenderableWidget(
			Button.builder(modText) { _ ->
				recording = modId
				rebuildWidgets()
			}.tooltip(
				Tooltip.create(
					Component.literal("Modifier A (default None). Click then press a key to record. Right-click sets None.")
				)
			).bounds(left, y, 150, 20).build()
		)
		addRenderableWidget(
			Button.builder(
				Component.literal("B: ").append(mapping.translatedKeyMessage)
			) { _ -> }.tooltip(
				Tooltip.create(Component.literal("Main key B. Change it in Controls → SpotFilter."))
			).bounds(left + 158, y, 160, 20).build()
		)
	}

	override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
		if (event.button() == 1) {
			val cfg = SpotFilterConfig.instance
			val yHit = event.y().toInt()
			val index = ((yHit - 64) / 24)
			val setter: ((Int) -> Unit)? = when (index) {
				0 -> { v -> cfg.modOpenFilter = v }
				1 -> { v -> cfg.modClearSpots = v }
				2 -> { v -> cfg.modToggleHud = v }
				3 -> { v -> cfg.modHighlightUp = v }
				4 -> { v -> cfg.modHighlightDown = v }
				5 -> { v -> cfg.modLockHighlight = v }
				6 -> { v -> cfg.modClearHighlights = v }
				else -> null
			}
			if (setter != null && event.x().toInt() in 12 until 162) {
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
				"Open Filter" -> cfg.modOpenFilter = event.key()
				"Clear spots" -> cfg.modClearSpots = event.key()
				"Toggle HUD" -> cfg.modToggleHud = event.key()
				"Highlight up" -> cfg.modHighlightUp = event.key()
				"Highlight down" -> cfg.modHighlightDown = event.key()
				"Lock highlight" -> cfg.modLockHighlight = event.key()
				"Clear highlights" -> cfg.modClearHighlights = event.key()
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
