package kcl.spotfilter

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object SpotFilter : ModInitializer {
	const val MOD_ID: String = "spotfilter"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("SpotFilter loaded")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
