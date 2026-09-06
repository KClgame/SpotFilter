package kcl.spotfilter.client

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.command.SpotCommands
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.highlight.HighlightState
import kcl.spotfilter.client.scan.SpotScanner
import kcl.spotfilter.client.ui.FilterScreen
import kcl.spotfilter.client.ui.SpotGuideOverlay
import kcl.spotfilter.client.ui.SpotHud
import kcl.spotfilter.client.ui.typingInBox
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object SpotFilterClient : ClientModInitializer {
	private val category: KeyMapping.Category =
		KeyMapping.Category.register(SpotFilter.id("main"))

	lateinit var openFilter: KeyMapping
		private set
	lateinit var clearSpots: KeyMapping
		private set
	lateinit var toggleHud: KeyMapping
		private set
	lateinit var highlightUp: KeyMapping
		private set
	lateinit var highlightDown: KeyMapping
		private set
	lateinit var lockHighlight: KeyMapping
		private set
	lateinit var clearHighlights: KeyMapping
		private set

	@Volatile
	private var openFilterPending = false

	fun requestOpenFilter() {
		openFilterPending = true
	}

	override fun onInitializeClient() {
		SpotFilterConfig.instance
		openFilter = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.open_filter", GLFW.GLFW_KEY_O, category)
		)
		clearSpots = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.clear_spots", GLFW.GLFW_KEY_P, category)
		)
		toggleHud = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.toggle_hud", GLFW.GLFW_KEY_L, category)
		)
		highlightUp = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.highlight_up", GLFW.GLFW_KEY_UP, category)
		)
		highlightDown = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.highlight_down", GLFW.GLFW_KEY_DOWN, category)
		)
		lockHighlight = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.lock_highlight", GLFW.GLFW_KEY_G, category)
		)
		clearHighlights = KeyMappingHelper.registerKeyMapping(
			KeyMapping("key.spotfilter.clear_highlights", GLFW.GLFW_KEY_H, category)
		)
		SpotHud.register()
		SpotGuideOverlay.register()
		kcl.spotfilter.client.world.PinnedSpotMarker.register()
		SpotCommands.register()
		registerGrottoChatRefresh()

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			if (openFilterPending) {
				val screen = client.gui.screen()
				if (screen !is net.minecraft.client.gui.screens.ChatScreen) {
					openFilterPending = false
					if (screen !is FilterScreen) {
						client.gui.setScreen(FilterScreen())
					}
				}
			}
			val cfg = SpotFilterConfig.instance
			if (Hotkeys.consume(openFilter, cfg.modOpenFilter)) {
				val screen = client.gui.screen()
				if (!screen.typingInBox()) {
					if (screen is FilterScreen) {
						client.gui.setScreen(null)
					} else if (screen == null) {
						client.gui.setScreen(FilterScreen())
					}
				}
			}
			if (Hotkeys.consume(clearSpots, cfg.modClearSpots)) {
				if (!client.gui.screen().typingInBox()) {
					SpotPool.clearSpots()
				}
			}
			if (Hotkeys.consume(toggleHud, cfg.modToggleHud)) {
				if (client.gui.screen() !is kcl.spotfilter.client.ui.PerkPickerScreen &&
					!client.gui.screen().typingInBox()
				) {
					cfg.hudVisible = !cfg.hudVisible
					SpotFilterConfig.save()
				}
			}
			if (client.gui.screen() == null) {
				if (Hotkeys.consume(highlightUp, cfg.modHighlightUp)) {
					HighlightState.move(-1)
				}
				if (Hotkeys.consume(highlightDown, cfg.modHighlightDown)) {
					HighlightState.move(1)
				}
				if (Hotkeys.consume(lockHighlight, cfg.modLockHighlight)) {
					HighlightState.toggleLock()
				}
				if (Hotkeys.consume(clearHighlights, cfg.modClearHighlights)) {
					HighlightState.clearLocks()
				}
			} else {
				while (highlightUp.consumeClick()) {}
				while (highlightDown.consumeClick()) {}
				while (lockHighlight.consumeClick()) {}
				while (clearHighlights.consumeClick()) {}
			}
			SpotScanner.tick(client)
		}
	}

	private fun registerGrottoChatRefresh() {
		val needle = "Your Grotto has become unstable"
		ClientReceiveMessageEvents.GAME.register { message, _ ->
			if (message.string.contains(needle, ignoreCase = true)) {
				SpotPool.refreshGrottoFromChat()
			}
		}
		ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
			if (message.string.contains(needle, ignoreCase = true)) {
				SpotPool.refreshGrottoFromChat()
			}
		}
	}
}
