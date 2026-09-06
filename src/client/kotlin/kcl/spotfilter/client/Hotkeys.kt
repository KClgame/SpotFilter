package kcl.spotfilter.client

import kcl.spotfilter.client.config.SpotFilterConfig
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component

object Hotkeys {
	const val NONE = 0

	fun isModifierDown(glfwKey: Int): Boolean {
		if (glfwKey == NONE) return true
		val window = Minecraft.getInstance().getWindow()
		return when (glfwKey) {
			InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT ->
				InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT) ||
					InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT)
			InputConstants.KEY_LALT, InputConstants.KEY_RALT ->
				InputConstants.isKeyDown(window, InputConstants.KEY_LALT) ||
					InputConstants.isKeyDown(window, InputConstants.KEY_RALT)
			InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL ->
				InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL) ||
					InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL)
			else -> InputConstants.isKeyDown(window, glfwKey)
		}
	}

	fun consume(mapping: KeyMapping, modifier: Int): Boolean {
		var fired = false
		while (mapping.consumeClick()) {
			if (isModifierDown(modifier)) fired = true
		}
		return fired
	}

	fun modifierLabel(glfwKey: Int): Component {
		if (glfwKey == NONE) return Component.literal("None")
		return InputConstants.Type.KEYSYM.getOrCreate(glfwKey).displayName
	}
}
