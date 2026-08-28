package kcl.spotfilter.client.filter

import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.data.StabilityCost
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
	GTE(">=", "At least"),
	LT("<", "Less than"),
	LTE("<=", "At most"),
	EQ("=", "Equal to"),
	BETWEEN("..", "Between")
}

enum class SortDir(val label: String) {
	DESC("High → Low"),
	ASC("Low → High")
}

class FilterSlot {
	var perk: PerkType? = null
	var compare: CompareOp = CompareOp.GT
	var threshold: Int = 10
	var thresholdMax: Int = 30
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
		return compareValues(value, threshold, thresholdMax, compare)
	}

	fun sortKey(spot: FishingSpot): Int {
		val type = perk ?: return 0
		return spot.perkValue(type)
	}

	fun compactLabel(): String {
		val type = perk ?: return "None"
		return if (type.hasVariableValue) {
			val range = if (compare == CompareOp.BETWEEN) {
				"${type.valueLabel(minOf(threshold, thresholdMax))}–${type.valueLabel(maxOf(threshold, thresholdMax))}"
			} else {
				"${compare.symbol} ${type.valueLabel(threshold)}"
			}
			"${type.displayName} $range  ${sortDir.label}"
		} else {
			"${type.displayName}  ${sortDir.label}"
		}
	}

	fun cycleCompare() {
		compare = when (compare) {
			CompareOp.GT -> CompareOp.GTE
			CompareOp.GTE -> CompareOp.LT
			CompareOp.LT -> CompareOp.LTE
			CompareOp.LTE -> CompareOp.EQ
			CompareOp.EQ -> CompareOp.BETWEEN
			CompareOp.BETWEEN -> CompareOp.GT
		}
	}

	fun cycleThreshold() {
		threshold = nextPerkValue(threshold)
	}

	fun cycleThresholdMax() {
		thresholdMax = nextPerkValue(thresholdMax)
	}

	fun cycleSort() {
		sortDir = if (sortDir == SortDir.DESC) SortDir.ASC else SortDir.DESC
	}

	fun clear() {
		perk = null
		compare = CompareOp.GT
		threshold = 10
		thresholdMax = 30
		sortDir = SortDir.DESC
	}

	fun copyFrom(other: FilterSlot) {
		perk = other.perk
		compare = other.compare
		threshold = other.threshold
		thresholdMax = other.thresholdMax
		sortDir = other.sortDir
	}
}

class StockFilter {
	var enabled: Boolean = false
	var compare: CompareOp = CompareOp.GT
	var level: StockLevel = StockLevel.HIGH
	var levelMax: StockLevel = StockLevel.PLENTIFUL

	fun matches(spot: FishingSpot): Boolean {
		if (!enabled) return true
		val rank = spot.stock?.rank ?: -1
		return compareValues(rank, level.rank, levelMax.rank, compare)
	}

	fun compactLabel(): String {
		if (!enabled) return "Stock: Off"
		return if (compare == CompareOp.BETWEEN) {
			val high = if (level.rank >= levelMax.rank) level else levelMax
			val low = if (level.rank >= levelMax.rank) levelMax else level
			"Stock ${high.label}–${low.label}"
		} else {
			"Stock ${compare.symbol} ${level.label}"
		}
	}

	fun cycleCompare() {
		compare = when (compare) {
			CompareOp.GT -> CompareOp.GTE
			CompareOp.GTE -> CompareOp.LT
			CompareOp.LT -> CompareOp.LTE
			CompareOp.LTE -> CompareOp.EQ
			CompareOp.EQ -> CompareOp.BETWEEN
			CompareOp.BETWEEN -> CompareOp.GT
		}
	}

	fun cycleLevel() {
		level = nextStock(level)
	}

	fun cycleLevelMax() {
		levelMax = nextStock(levelMax)
	}
}

class StabilityFilter {
	var enabled: Boolean = false
	var compare: CompareOp = CompareOp.EQ
	var level: StabilityCost = StabilityCost.LOW
	var levelMax: StabilityCost = StabilityCost.HIGH

	fun matches(spot: FishingSpot): Boolean {
		if (!enabled) return true
		val rank = spot.stability?.rank ?: return false
		return compareValues(rank, level.rank, levelMax.rank, compare)
	}

	fun compactLabel(): String {
		if (!enabled) return "Cost: Off"
		return if (compare == CompareOp.BETWEEN) {
			val high = if (level.rank >= levelMax.rank) level else levelMax
			val low = if (level.rank >= levelMax.rank) levelMax else level
			"Cost ${high.label}–${low.label}"
		} else {
			"Cost ${compare.symbol} ${level.label}"
		}
	}

	fun cycleCompare() {
		compare = when (compare) {
			CompareOp.GT -> CompareOp.GTE
			CompareOp.GTE -> CompareOp.LT
			CompareOp.LT -> CompareOp.LTE
			CompareOp.LTE -> CompareOp.EQ
			CompareOp.EQ -> CompareOp.BETWEEN
			CompareOp.BETWEEN -> CompareOp.GT
		}
	}

	fun cycleLevel() {
		level = nextStability(level)
	}

	fun cycleLevelMax() {
		levelMax = nextStability(levelMax)
	}
}

class AutoPinRule {
	var name: String = "Rule"
	var enabled: Boolean = true
	var mode: FilterMode = FilterMode.AND
	val slots: Array<FilterSlot> = arrayOf(FilterSlot(), FilterSlot(), FilterSlot())
	val stock: StockFilter = StockFilter()
	val stability: StabilityFilter = StabilityFilter()
	var customColorHex: String = ""
	var nickname: String = ""

	fun matches(spot: FishingSpot, useStability: Boolean): Boolean {
		if (!stock.matches(spot)) return false
		if (useStability && !stability.matches(spot)) return false
		val active = slots.filter { it.isActive }
		if (active.isEmpty()) return stock.enabled || (useStability && stability.enabled)
		return if (mode == FilterMode.AND) {
			active.all { it.matches(spot) }
		} else {
			active.any { it.matches(spot) }
		}
	}

	fun customRgb(): Int? = parseHexColor(customColorHex)
}

class FilterProfile {
	var mode: FilterMode = FilterMode.AND
	val slots: Array<FilterSlot> = arrayOf(FilterSlot(), FilterSlot(), FilterSlot())
	val stock: StockFilter = StockFilter()
	val stability: StabilityFilter = StabilityFilter()
	val autoPinRules: MutableList<AutoPinRule> = ArrayList()
}

fun compareValues(value: Int, min: Int, max: Int, compare: CompareOp): Boolean {
	val lo = minOf(min, max)
	val hi = maxOf(min, max)
	return when (compare) {
		CompareOp.GT -> value > min
		CompareOp.GTE -> value >= min
		CompareOp.LT -> value < min
		CompareOp.LTE -> value <= min
		CompareOp.EQ -> value == min
		CompareOp.BETWEEN -> value in lo..hi
	}
}

fun nextPerkValue(current: Int): Int = when (current) {
	10 -> 20
	20 -> 30
	else -> 10
}

fun nextStock(current: StockLevel): StockLevel {
	val levels = StockLevel.entries
	return levels[(levels.indexOf(current) + 1) % levels.size]
}

fun nextStability(current: StabilityCost): StabilityCost {
	val levels = StabilityCost.entries
	return levels[(levels.indexOf(current) + 1) % levels.size]
}

fun parseHexColor(raw: String): Int? {
	val text = raw.trim().removePrefix("#")
	if (text.length != 6) return null
	return text.toIntOrNull(16)
}

object FilterState {
	var kind: SpotKind = SpotKind.NORMAL
	val normal = FilterProfile()
	val grotto = FilterProfile()

	val active: FilterProfile
		get() = if (kind == SpotKind.GROTTO) grotto else normal

	var mode: FilterMode
		get() = active.mode
		set(value) {
			active.mode = value
		}
	val slots: Array<FilterSlot>
		get() = active.slots
	val stock: StockFilter
		get() = active.stock
	val stability: StabilityFilter
		get() = active.stability
	val autoPinRules: MutableList<AutoPinRule>
		get() = active.autoPinRules

	fun toggleMode() {
		mode = if (mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
	}

	fun toggleKind() {
		kind = if (kind == SpotKind.GROTTO) SpotKind.NORMAL else SpotKind.GROTTO
	}

	fun profileFor(spot: FishingSpot): FilterProfile =
		if (spot.kind == SpotKind.GROTTO) grotto else normal

	fun matches(spot: FishingSpot): Boolean {
		if (spot.kind != kind) return false
		if (!stock.matches(spot)) return false
		if (kind == SpotKind.GROTTO && !stability.matches(spot)) return false
		val activeSlots = slots.filter { it.isActive }
		if (activeSlots.isEmpty()) return true
		return if (mode == FilterMode.AND) {
			activeSlots.all { it.matches(spot) }
		} else {
			activeSlots.any { it.matches(spot) }
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
				if (kind == SpotKind.GROTTO) {
					val costCmp = (b.stability?.rank ?: 0).compareTo(a.stability?.rank ?: 0)
					if (costCmp != 0) return@sortedWith costCmp
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
		if (spot.stock == StockLevel.DEPLETED) {
			if (spot.autoPinned) {
				SpotPool.setPinned(spot, false)
			}
			return
		}
		val profile = FilterState.profileFor(spot)
		val grotto = spot.kind == SpotKind.GROTTO
		val rule = profile.autoPinRules.firstOrNull { it.enabled && it.matches(spot, grotto) }
		if (rule != null) {
			spot.pinColorOverride = rule.customRgb()
			spot.autoPinned = true
			SpotPool.assignGroup(spot, rule.nickname)
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
