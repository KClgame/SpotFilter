package kcl.spotfilter.client.ui

import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen

fun Screen?.typingInBox(): Boolean {
	val screen = this ?: return false
	val focused = screen.getFocused()
	if (focused is EditBox && focused.canConsumeInput()) return true
	return screen.children().any { child -> child is EditBox && child.canConsumeInput() }
}
