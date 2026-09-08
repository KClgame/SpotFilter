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

	private var cursorId = 0
	private val locked = HashSet<Int>()

	fun prune() {
		val spots = SpotPool.pinned()
		if (spots.isEmpty()) {
			cursorId = 0
			locked.clear()
			return
		}
		val ids = spots.map { it.id }.toHashSet()
		locked.removeAll { it !in ids }
		if (cursorId != 0 && cursorId !in ids) cursorId = 0
	}

	fun spots(): List<FishingSpot> = SpotPool.pinned()

	fun current(): FishingSpot? {
		if (cursorId == 0) return null
		return spots().firstOrNull { it.id == cursorId }
	}

	fun move(delta: Int) {
		val spots = spots()
		if (spots.isEmpty()) {
			cursorId = 0
			return
		}
		val index = spots.indexOfFirst { it.id == cursorId }
		val start = if (index >= 0) index else if (delta > 0) -1 else spots.size
		cursorId = spots[Math.floorMod(start + delta, spots.size)].id
	}

	fun toggleLock() {
		val spot = current() ?: return
		if (!locked.add(spot.id)) locked.remove(spot.id)
	}

	fun visualsOn(): Boolean = SpotFilterConfig.instance.highlightEnabled

	fun isCursor(spot: FishingSpot): Boolean = current()?.id == spot.id

	fun isLit(spot: FishingSpot): Boolean =
		visualsOn() && (isCursor(spot) || spot.id in locked)

	fun litSpots(): List<FishingSpot> {
		val spots = spots()
		if (spots.isEmpty()) return emptyList()
		return spots.filter { isLit(it) }
	}

	fun clearLocks() {
		locked.clear()
	}

	fun clear() {
		cursorId = 0
		locked.clear()
	}

	fun hudPinned(): List<FishingSpot> {
		prune()
		return SpotPool.pinned()
	}

	fun visiblePinned(): List<FishingSpot> {
		prune()
		val pinned = SpotPool.pinned()
		if (!visualsOn() || SpotFilterConfig.instance.highlightMode() != HighlightMode.SOLO) return pinned
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
