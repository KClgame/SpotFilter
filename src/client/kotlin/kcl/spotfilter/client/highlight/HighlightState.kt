package kcl.spotfilter.client.highlight

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

object HighlightState {
	const val ORANGE = 0xFFAA55
	const val ORANGE_ARGB = 0xFFFFAA55.toInt()

	private var cursor = 0
	private val locked = HashSet<Int>()

	fun prune() {
		val spots = SpotPool.pinned()
		if (spots.isEmpty()) {
			cursor = 0
			locked.clear()
			return
		}
		val ids = spots.map { it.id }.toHashSet()
		locked.removeAll { it !in ids }
		cursor = cursor.coerceIn(0, spots.lastIndex)
	}

	fun spots(): List<FishingSpot> = SpotPool.pinned()

	fun current(): FishingSpot? {
		val spots = spots()
		if (spots.isEmpty()) return null
		return spots[cursor.coerceIn(0, spots.lastIndex)]
	}

	fun move(delta: Int) {
		val spots = spots()
		if (spots.isEmpty()) {
			cursor = 0
			return
		}
		val size = spots.size
		cursor = Math.floorMod(cursor.coerceIn(0, size - 1) + delta, size)
	}

	fun toggleLock() {
		val spot = current() ?: return
		if (!locked.add(spot.id)) locked.remove(spot.id)
	}

	fun isCursor(spot: FishingSpot): Boolean = current()?.id == spot.id

	fun isLit(spot: FishingSpot): Boolean =
		isCursor(spot) || spot.id in locked

	fun litSpots(): List<FishingSpot> {
		val spots = spots()
		if (spots.isEmpty()) return emptyList()
		return spots.filter { isLit(it) }
	}

	fun clearLocks() {
		locked.clear()
	}

	fun clear() {
		cursor = 0
		locked.clear()
	}

	fun visiblePinned(): List<FishingSpot> {
		prune()
		val pinned = SpotPool.pinned()
		if (SpotFilterConfig.instance.highlightMode() != HighlightMode.SOLO) return pinned
		return pinned.filter { isLit(it) }
	}

	fun brighten(rgb: Int, amount: Float = 0.55f): Int {
		val r = (rgb shr 16) and 0xFF
		val g = (rgb shr 8) and 0xFF
		val b = rgb and 0xFF
		fun lift(c: Int) = (c + ((255 - c) * amount)).toInt().coerceIn(0, 255)
		return (lift(r) shl 16) or (lift(g) shl 8) or lift(b)
	}

	fun tintWhites(component: Component): Component {
		val out = Component.empty()
		component.visit({ style: Style, text: String ->
			if (text.isNotEmpty()) {
				val rgb = style.color?.value
				val colored = if (rgb == null || rgb == 0xFFFFFF) {
					style.withColor(ORANGE)
				} else {
					style
				}
				out.append(Component.literal(text).withStyle(colored))
			}
			Optional.empty<Any>()
		}, Style.EMPTY)
		return out
	}
}
