package kcl.spotfilter.client

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.command.SpotCommands
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.scan.SpotScanner
import kcl.spotfilter.client.ui.FilterScreen
import kcl.spotfilter.client.ui.SpotHud
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
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
		kcl.spotfilter.client.world.PinnedSpotMarker.register()
		SpotCommands.register()

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			while (openFilter.consumeClick()) {
				if (client.gui.screen() is FilterScreen) {
					client.gui.setScreen(null)
				} else if (client.gui.screen() == null) {
					client.gui.setScreen(FilterScreen())
				}
			}
			while (clearSpots.consumeClick()) {
				SpotPool.clearSpots()
			}
			while (toggleHud.consumeClick()) {
				if (client.gui.screen() is kcl.spotfilter.client.ui.PerkPickerScreen) continue
				SpotFilterConfig.instance.hudVisible = !SpotFilterConfig.instance.hudVisible
				SpotFilterConfig.save()
			}
			SpotScanner.tick(client)
		}
	}
}
