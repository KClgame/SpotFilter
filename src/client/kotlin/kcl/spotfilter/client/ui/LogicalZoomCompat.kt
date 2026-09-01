package kcl.spotfilter.client.ui

import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Logical Zoom scales the world projection matrix in `GameRenderer.renderLevel`
 * (`projection.scale(1/zoomLevel, 1/zoomLevel, 1)`) and does not change `Camera.fov`.
 * HUD `projectPointToScreen` still uses the unzoomed camera matrix, so guide text
 * stays at the pre-zoom angle. Apply the same XY scale to NDC when that mod is zooming.
 */
object LogicalZoomCompat {
	private val isZooming: Method?
	private val zoomLevel: Field?

	init {
		var method: Method? = null
		var field: Field? = null
		try {
			val clazz = Class.forName("com.logicalgeekboy.logical_zoom.LogicalZoom")
			method = clazz.getMethod("isZooming")
			field = clazz.getField("zoomLevel")
		} catch (_: Throwable) {
		}
		isZooming = method
		zoomLevel = field
	}

	fun ndcXyScale(): Float {
		val method = isZooming ?: return 1f
		return try {
			if (method.invoke(null) != true) return 1f
			val level = zoomLevel?.getFloat(null) ?: 0.23f
			if (level > 1e-4f) 1f / level else 1f
		} catch (_: Throwable) {
			1f
		}
	}
}
