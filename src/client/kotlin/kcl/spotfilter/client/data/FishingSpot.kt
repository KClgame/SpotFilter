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

enum class SpotKind(val label: String) {
	NORMAL("Normal"),
	GROTTO("Grotto")
}

enum class StockLevel(val label: String, val rank: Int, val rgb: Int) {
	PLENTIFUL("Plentiful", 5, 0xA770FE),
	VERY_HIGH("Very High", 4, 0x55FFFF),
	HIGH("High", 3, 0x55FF55),
	MEDIUM("Medium", 2, 0xFFD83D),
	LOW("Low", 1, 0xFF8C1A),
	DEPLETED("Depleted", 0, 0x888888);

	companion object {
		fun fromLabel(raw: String): StockLevel? =
			entries.firstOrNull { it.label.equals(raw.trim(), ignoreCase = true) }
	}
}

enum class StabilityCost(val label: String, val rank: Int, val rgb: Int) {
	LOW("Low", 2, 0x65FEFE),
	MEDIUM("Medium", 1, 0x55FE56),
	HIGH("High", 0, 0xFEFE55);

	companion object {
		fun fromRgb(rgb: Int): StabilityCost? {
			val best = entries.minBy { rgbDistance(it.rgb, rgb) }
			return if (rgbDistance(best.rgb, rgb) <= 48) best else null
		}

		private fun rgbDistance(a: Int, b: Int): Int {
			val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
			val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
			val db = (a and 0xFF) - (b and 0xFF)
			return kotlin.math.abs(dr) + kotlin.math.abs(dg) + kotlin.math.abs(db)
		}
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
	var kind: SpotKind = SpotKind.NORMAL,
	var stability: StabilityCost? = null,
	var stabilityRgb: Int? = null,
	var stabilityRange: String? = null,
	var pinned: Boolean = false,
	var autoPinned: Boolean = false,
	var pinColorOverride: Int? = null,
	var nickname: String? = null,
	var groupIndex: Int = 0,
	var rank: Int = 0
) {
	fun grottoChance(): ParsedPerk? = PerkPriority.grottoChance(perks)

	fun primaryPerk(): ParsedPerk? =
		if (kind == SpotKind.GROTTO) PerkPriority.grottoDisplay(perks) else PerkPriority.primary(perks)

	fun spotTypeLabel(): String {
		if (kind != SpotKind.GROTTO) return "fishing"
		return when (grottoChance()?.type) {
			PerkType.FISH_CHANCE -> "fish"
			PerkType.PEARL_CHANCE -> "pearl"
			PerkType.TREASURE_CHANCE -> "treasure"
			PerkType.SPIRIT_CHANCE -> "spirit"
			else -> "fishing"
		}
	}

	fun grottoFamilyOrder(): Int = when (grottoChance()?.type) {
		PerkType.FISH_CHANCE -> 0
		PerkType.PEARL_CHANCE -> 1
		PerkType.TREASURE_CHANCE -> 2
		PerkType.SPIRIT_CHANCE -> 3
		else -> 4
	}

	fun grottoBonusScore(): Int {
		val family = grottoChance()?.type?.family
		val bonuses = perks.filter { !it.type.isGrottoChance && !it.type.skipsSpotColor }
		val maxVal = bonuses.maxOfOrNull { it.value } ?: 0
		val familyBonuses = bonuses.filter { it.type.family == family }
		val familyMax = familyBonuses.maxOfOrNull { it.value } ?: 0
		val magnet = if (familyBonuses.any { it.type.kind == kcl.spotfilter.client.parse.PerkKind.MAGNET }) 2 else 0
		val hook = if (familyBonuses.any { it.type.kind == kcl.spotfilter.client.parse.PerkKind.HOOK }) 1 else 0
		return maxVal * 1000 + familyMax * 20 + magnet + hook + bonuses.size
	}

	fun perkValue(type: PerkType): Int =
		perks.firstOrNull { it.type == type }?.value ?: -1

	fun hasPerk(type: PerkType): Boolean =
		perks.any { it.type == type }

	fun stockDisplayRgb(): Int = stockRgb ?: stock?.rgb ?: 0xAAAAAA

	fun stabilityDisplayRgb(): Int = stabilityRgb ?: stability?.rgb ?: 0xAAAAAA

	fun rankNumber(): Int = if (rank > 0) rank else id

	fun displayTitle(): String =
		if (!nickname.isNullOrBlank()) {
			"$nickname #${rankNumber()}"
		} else {
			"${spotTypeLabel()} spot #${rankNumber()}"
		}

	fun guideLabel(): String =
		if (!nickname.isNullOrBlank()) {
			"$nickname #${rankNumber()}"
		} else {
			"${spotTypeLabel()} spot #${rankNumber()}"
		}

	fun markerRgb(): Int {
		pinColorOverride?.let { return it }
		if (kind == SpotKind.GROTTO) {
			val chance = grottoChance()
			return chance?.resolvedNameRgb() ?: chance?.type?.family?.rgb ?: 0xFFFFFF
		}
		return primaryPerk()?.type?.family?.rgb ?: 0xFFFFFF
	}
}
