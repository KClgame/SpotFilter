package kcl.spotfilter.client.ui

import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen

fun Screen?.typingInBox(): Boolean {
	val screen = this ?: return false
	val focused = screen.getFocused()
	if (focused is EditBox && focused.canConsumeInput()) return true
	return screen.children().any { child -> child is EditBox && child.canConsumeInput() }
}

/** List rows to move for a wheel event. Fractional deltas (high-res mice / hotbar-scroll mods) still move at least one row. */
fun wheelRows(scrollY: Double): Int {
	if (scrollY == 0.0) return 0
	val dir = if (scrollY > 0.0) 1 else -1
	val mag = kotlin.math.abs(scrollY)
	return dir * mag.coerceAtLeast(1.0).toInt()
}
