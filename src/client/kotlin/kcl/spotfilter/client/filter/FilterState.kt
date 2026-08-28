package kcl.spotfilter.client.filter

import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.data.StockLevel
import kcl.spotfilter.client.parse.PerkType
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

enum class FilterMode {
	AND,
	OR
}

enum class CompareOp(val symbol: String, val label: String) {
	GT(">", "Greater than"),
	LT("<", "Less than"),
	EQ("=", "Equal to")
}

enum class SortDir(val label: String) {
	DESC("High → Low"),
	ASC("Low → High")
}

class FilterSlot {
	var perk: PerkType? = null
	var compare: CompareOp = CompareOp.GT
	var threshold: Int = 10
	var sortDir: SortDir = SortDir.DESC

	val isActive: Boolean
		get() = perk != null

	fun matches(spot: FishingSpot): Boolean {
		val type = perk ?: return true
		if (!type.hasVariableValue) {
			return spot.hasPerk(type)
		}
		val value = spot.perkValue(type)
		if (value < 0) return false
		return compareValues(value, threshold, compare)
	}

	fun sortKey(spot: FishingSpot): Int {
		val type = perk ?: return 0
		return spot.perkValue(type)
	}

	fun compactLabel(): String {
		val type = perk ?: return "None"
		return if (type.hasVariableValue) {
			"${type.displayName} ${compare.symbol} ${type.valueLabel(threshold).removePrefix("+")}  ${sortDir.label}"
		} else {
			"${type.displayName}  ${sortDir.label}"
		}
	}

	fun cycleCompare() {
		compare = when (compare) {
			CompareOp.GT -> CompareOp.LT
			CompareOp.LT -> CompareOp.EQ
			CompareOp.EQ -> CompareOp.GT
		}
	}

	fun cycleThreshold() {
		threshold = when (threshold) {
			10 -> 20
			20 -> 30
			else -> 10
		}
	}

	fun cycleSort() {
		sortDir = if (sortDir == SortDir.DESC) SortDir.ASC else SortDir.DESC
	}

	fun clear() {
		perk = null
		compare = CompareOp.GT
		threshold = 10
		sortDir = SortDir.DESC
	}

	fun copyFrom(other: FilterSlot) {
		perk = other.perk
		compare = other.compare
		threshold = other.threshold
		sortDir = other.sortDir
	}
}

class StockFilter {
	var enabled: Boolean = false
	var compare: CompareOp = CompareOp.GT
	var level: StockLevel = StockLevel.HIGH

	fun matches(spot: FishingSpot): Boolean {
		if (!enabled) return true
		val rank = spot.stock?.rank ?: -1
		return compareValues(rank, level.rank, compare)
	}

	fun compactLabel(): String =
		if (!enabled) "Stock: Off"
		else "Stock ${compare.symbol} ${level.label}"

	fun cycleCompare() {
		compare = when (compare) {
			CompareOp.GT -> CompareOp.LT
			CompareOp.LT -> CompareOp.EQ
			CompareOp.EQ -> CompareOp.GT
		}
	}

	fun cycleLevel() {
		val levels = StockLevel.entries
		level = levels[(levels.indexOf(level) + 1) % levels.size]
	}
}

class AutoPinRule {
	var name: String = "Rule"
	var enabled: Boolean = true
	var mode: FilterMode = FilterMode.AND
	val slots: Array<FilterSlot> = arrayOf(FilterSlot(), FilterSlot(), FilterSlot())
	val stock: StockFilter = StockFilter()
	var customColorHex: String = ""

	fun matches(spot: FishingSpot): Boolean {
		if (!stock.matches(spot)) return false
		val active = slots.filter { it.isActive }
		if (active.isEmpty()) return stock.enabled
		return if (mode == FilterMode.AND) {
			active.all { it.matches(spot) }
		} else {
			active.any { it.matches(spot) }
		}
	}

	fun customRgb(): Int? = parseHexColor(customColorHex)
}

fun compareValues(value: Int, threshold: Int, compare: CompareOp): Boolean =
	when (compare) {
		CompareOp.GT -> value > threshold
		CompareOp.LT -> value < threshold
		CompareOp.EQ -> value == threshold
	}

fun parseHexColor(raw: String): Int? {
	val text = raw.trim().removePrefix("#")
	if (text.length != 6) return null
	return text.toIntOrNull(16)
}

object FilterState {
	var mode: FilterMode = FilterMode.AND
	val slots: Array<FilterSlot> = arrayOf(FilterSlot(), FilterSlot(), FilterSlot())
	val stock: StockFilter = StockFilter()
	val autoPinRules: MutableList<AutoPinRule> = ArrayList()

	fun toggleMode() {
		mode = if (mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
	}

	fun matches(spot: FishingSpot): Boolean {
		if (!stock.matches(spot)) return false
		val active = slots.filter { it.isActive }
		if (active.isEmpty()) return true
		return if (mode == FilterMode.AND) {
			active.all { it.matches(spot) }
		} else {
			active.any { it.matches(spot) }
		}
	}

	fun filteredSorted(): List<FishingSpot> {
		val player = Minecraft.getInstance().player
		val origin = player?.position() ?: Vec3.ZERO
		return SpotPool.all()
			.filter { matches(it) }
			.sortedWith { a, b ->
				for (slot in slots) {
					if (!slot.isActive) continue
					val cmp = slot.sortKey(a).compareTo(slot.sortKey(b))
					if (cmp != 0) {
						return@sortedWith if (slot.sortDir == SortDir.DESC) -cmp else cmp
					}
				}
				val stockCmp = (b.stock?.rank ?: 0).compareTo(a.stock?.rank ?: 0)
				if (stockCmp != 0) return@sortedWith stockCmp
				val dist = distSq(a, origin).compareTo(distSq(b, origin))
				if (dist != 0) return@sortedWith dist
				a.id.compareTo(b.id)
			}
	}

	private fun distSq(spot: FishingSpot, origin: Vec3): Double {
		val dx = (spot.x + 0.5) - origin.x
		val dy = spot.y - origin.y
		val dz = (spot.z + 0.5) - origin.z
		return dx * dx + dy * dy + dz * dz
	}
}

object AutoPin {
	fun apply(spot: FishingSpot) {
		val rule = FilterState.autoPinRules.firstOrNull { it.enabled && it.matches(spot) }
		if (rule != null) {
			spot.pinColorOverride = rule.customRgb()
			spot.autoPinned = true
			if (!spot.pinned) {
				SpotPool.setPinned(spot, true)
			} else {
				kcl.spotfilter.client.world.PinnedSpotMarker.sync(spot)
			}
		} else if (spot.autoPinned) {
			spot.pinColorOverride = null
			spot.autoPinned = false
			if (spot.pinned) {
				SpotPool.setPinned(spot, false)
			}
		}
	}

	fun applyAll() {
		SpotPool.all().forEach { apply(it) }
	}
}
