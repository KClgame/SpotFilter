package kcl.spotfilter.client

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.command.SpotCommands
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotPool
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
			while (openFilter.consumeClick()) {
				val screen = client.gui.screen()
				if (screen.typingInBox()) continue
				if (screen is FilterScreen) {
					client.gui.setScreen(null)
				} else if (screen == null) {
					client.gui.setScreen(FilterScreen())
				}
			}
			while (clearSpots.consumeClick()) {
				if (client.gui.screen().typingInBox()) continue
				SpotPool.clearSpots()
			}
			while (toggleHud.consumeClick()) {
				if (client.gui.screen() is kcl.spotfilter.client.ui.PerkPickerScreen) continue
				if (client.gui.screen().typingInBox()) continue
				SpotFilterConfig.instance.hudVisible = !SpotFilterConfig.instance.hudVisible
				SpotFilterConfig.save()
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
