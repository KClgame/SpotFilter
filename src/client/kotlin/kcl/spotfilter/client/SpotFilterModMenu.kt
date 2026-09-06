package kcl.spotfilter.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import kcl.spotfilter.client.ui.FilterScreen

class SpotFilterModMenu : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
		ConfigScreenFactory { parent -> FilterScreen(parent) }
}
