package kcl.spotfilter.client.filter

import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
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

	fun matches(spot: FishingSpot): Boolean {
		val type = perk ?: return true
		if (!type.hasVariableValue) {
			return spot.hasPerk(type)
		}
		val value = spot.perkValue(type)
		if (value < 0) return false
		return when (compare) {
			CompareOp.GT -> value > threshold
			CompareOp.LT -> value < threshold
			CompareOp.EQ -> value == threshold
		}
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
}

object FilterState {
	var mode: FilterMode = FilterMode.AND
	val slots: Array<FilterSlot> = arrayOf(FilterSlot(), FilterSlot(), FilterSlot())

	fun toggleMode() {
		mode = if (mode == FilterMode.AND) FilterMode.OR else FilterMode.AND
	}

	fun matches(spot: FishingSpot): Boolean {
		val active = slots.filter { it.perk != null }
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
					val type = slot.perk ?: continue
					val av = a.perkValue(type)
					val bv = b.perkValue(type)
					val cmp = av.compareTo(bv)
					if (cmp != 0) {
						return@sortedWith if (slot.sortDir == SortDir.DESC) -cmp else cmp
					}
				}
				val stock = (b.stock?.rank ?: 0).compareTo(a.stock?.rank ?: 0)
				if (stock != 0) return@sortedWith stock
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
