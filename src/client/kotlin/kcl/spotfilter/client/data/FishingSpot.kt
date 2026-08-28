package kcl.spotfilter.client.data

import kcl.spotfilter.client.parse.ParsedPerk
import kcl.spotfilter.client.parse.PerkPriority
import kcl.spotfilter.client.parse.PerkType
import net.minecraft.resources.Identifier

data class SpotKey(
	val dimension: Identifier,
	val x: Int,
	val y: Int,
	val z: Int
)

enum class StockLevel(val label: String, val rank: Int, val rgb: Int) {
	PLENTIFUL("Plentiful", 5, 0xA770FE),
	VERY_HIGH("Very High", 4, 0x55FFFF),
	HIGH("High", 3, 0x55FF55),
	MEDIUM("Medium", 2, 0xFFD83D),
	LOW("Low", 1, 0xFF8C1A);

	companion object {
		fun fromLabel(raw: String): StockLevel? =
			entries.firstOrNull { it.label.equals(raw.trim(), ignoreCase = true) }
	}
}

data class FishingSpot(
	val key: SpotKey,
	var id: Int = 0,
	var entityId: Int,
	var x: Int,
	var y: Int,
	var z: Int,
	var stock: StockLevel?,
	var stockRgb: Int? = null,
	var perks: List<ParsedPerk>,
	var lastSeenGameTime: Long,
	var pinned: Boolean = false
) {
	fun primaryPerk(): ParsedPerk? = PerkPriority.primary(perks)

	fun perkValue(type: PerkType): Int =
		perks.firstOrNull { it.type == type }?.value ?: -1

	fun hasPerk(type: PerkType): Boolean =
		perks.any { it.type == type }

	fun stockDisplayRgb(): Int = stockRgb ?: stock?.rgb ?: 0xAAAAAA

	fun markerRgb(): Int = primaryPerk()?.resolvedNameRgb() ?: 0xFFFFFF
}
