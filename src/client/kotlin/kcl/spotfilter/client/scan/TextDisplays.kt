package kcl.spotfilter.client.scan

import kcl.spotfilter.client.mixin.TextDisplayAccessor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display

object TextDisplays {
	const val FLAG_SEE_THROUGH: Byte = 2

	fun readText(entity: Display.TextDisplay): Component {
		return (entity as TextDisplayAccessor).`spotfilter$getText`()
	}

	fun writeText(entity: Display.TextDisplay, text: Component) {
		(entity as TextDisplayAccessor).`spotfilter$setText`(text)
	}

	fun setSeeThrough(entity: Display.TextDisplay) {
		val accessor = entity as TextDisplayAccessor
		val flags = accessor.`spotfilter$getFlags`()
		accessor.`spotfilter$setFlags`((flags.toInt() or FLAG_SEE_THROUGH.toInt()).toByte())
	}
}
