package kcl.spotfilter.client.data

import kcl.spotfilter.client.parse.ParsedPerk
import kcl.spotfilter.client.parse.PerkFamily
import kcl.spotfilter.client.parse.PerkKind
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

/** Grotto Stability Cost bands from `(lo)-(hi)%`. Fish subtypes and pearl/treasure/spirit each have a table. */
object GrottoFishCost {
	fun resolve(perks: List<ParsedPerk>, lo: Int?, hi: Int?): StabilityCost? {
		if (lo == null || hi == null) return null
		val key = lo to hi
		val typed = tableFor(perks)
		if (typed != null) return typed[key]
		return PURE[key] ?: MAGNET_200[key] ?: ELUSIVE_10[key]
			?: PEARL[key] ?: SPIRIT[key] ?: TREASURE[key]
	}

	private fun tableFor(perks: List<ParsedPerk>): Map<Pair<Int, Int>, StabilityCost>? {
		val fish = perks.any { it.type == PerkType.FISH_CHANCE }
		return when {
			fish && perks.any { it.type == PerkType.FISH_MAGNET && it.value == 200 } -> MAGNET_200
			fish && perks.any { it.type == PerkType.ELUSIVE_CHANCE && it.value == 10 } -> ELUSIVE_10
			fish -> PURE
			perks.any { it.type == PerkType.PEARL_CHANCE } -> PEARL
			perks.any { it.type == PerkType.TREASURE_CHANCE } -> TREASURE
			perks.any { it.type == PerkType.SPIRIT_CHANCE } -> SPIRIT
			else -> null
		}
	}

	private val PURE = mapOf(
		(0 to 2) to StabilityCost.LOW,
		(0 to 3) to StabilityCost.MEDIUM,
		(1 to 3) to StabilityCost.HIGH
	)
	private val MAGNET_200 = mapOf(
		(1 to 4) to StabilityCost.LOW,
		(2 to 5) to StabilityCost.MEDIUM,
		(3 to 7) to StabilityCost.HIGH
	)
	private val ELUSIVE_10 = mapOf(
		(1 to 5) to StabilityCost.LOW,
		(2 to 7) to StabilityCost.MEDIUM,
		(3 to 9) to StabilityCost.HIGH
	)
	private val PEARL = mapOf(
		(1 to 5) to StabilityCost.LOW,
		(2 to 7) to StabilityCost.MEDIUM,
		(3 to 9) to StabilityCost.HIGH
	)
	private val SPIRIT = mapOf(
		(2 to 5) to StabilityCost.LOW,
		(3 to 7) to StabilityCost.MEDIUM,
		(5 to 10) to StabilityCost.HIGH
	)
	private val TREASURE = mapOf(
		(5 to 9) to StabilityCost.LOW,
		(7 to 13) to StabilityCost.MEDIUM,
		(10 to 20) to StabilityCost.HIGH
	)
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
	var place: FishingPlace? = null,
	var stability: StabilityCost? = null,
	var stabilityRgb: Int? = null,
	var stabilityRange: String? = null,
	var pinned: Boolean = false,
	var autoPinned: Boolean = false,
	var autoPinDecided: Boolean = false,
	var pinColorOverride: Int? = null,
	var nickname: String? = null,
	var groupIndex: Int = 0,
	var rank: Int = 0
) {
	fun grottoChance(): ParsedPerk? = PerkPriority.grottoChance(perks)

	fun primaryPerk(): ParsedPerk? =
		if (kind == SpotKind.GROTTO) PerkPriority.grottoDisplay(perks) else PerkPriority.primary(perks)

	fun familyGroup(): Int {
		if (kind == SpotKind.GROTTO) {
			return when (grottoChance()?.type) {
				PerkType.FISH_CHANCE -> 0
				PerkType.PEARL_CHANCE -> 1
				PerkType.TREASURE_CHANCE -> 2
				PerkType.SPIRIT_CHANCE -> 3
				else -> 4
			}
		}
		perks.firstOrNull { it.type.isTicket }?.type?.ticketGroup?.let { return it }
		val candidates = perks.mapNotNull { perk ->
			val index = perk.type.groupingIndex ?: return@mapNotNull null
			perk to index
		}
		if (candidates.isEmpty()) return 4
		return candidates.maxWith(
			compareBy<Pair<ParsedPerk, Int>> { it.first.value }
				.thenBy { if (it.first.type.kind == PerkKind.MAGNET) 1 else 0 }
				.thenBy { -it.second }
		).second
	}

	fun spotTypeLabel(): String = when (familyGroup()) {
		0 -> "fish"
		1 -> "pearl"
		2 -> "treasure"
		3 -> "spirit"
		4 -> "White Ticket"
		5 -> "Red Ticket"
		6 -> "Blue Ticket"
		7 -> "Purple Ticket"
		8 -> "Green Ticket"
		else -> "fishing"
	}

	fun customName(): String? = nickname?.trim()?.takeIf { it.isNotEmpty() }

	fun groupLabel(): String {
		customName()?.let { return it }
		if (familyGroup() in 4..8) return spotTypeLabel()
		return "${spotTypeLabel()} spot"
	}

	fun groupBucket(): Int = if (customName() != null) 0 else 1 + familyGroup()

	fun grottoFamilyOrder(): Int = familyGroup()

	fun grottoBonusScore(): Int {
		val group = familyGroup()
		val bonuses = perks.filter { !it.type.isGrottoChance && !it.type.skipsSpotColor }
		val maxVal = bonuses.maxOfOrNull { it.value } ?: 0
		val familyBonuses = bonuses.filter { it.type.groupingIndex == group }
		val familyMax = familyBonuses.maxOfOrNull { it.value } ?: 0
		val magnet = if (familyBonuses.any { it.type.kind == PerkKind.MAGNET }) 2 else 0
		val hook = if (familyBonuses.any { it.type.kind == PerkKind.HOOK }) 1 else 0
		return maxVal * 1000 + familyMax * 20 + magnet + hook + bonuses.size
	}

	fun perkValue(type: PerkType): Int =
		perks.firstOrNull { it.type == type }?.value ?: -1

	fun hasPerk(type: PerkType): Boolean =
		perks.any { it.type == type }

	fun pairTypes(): Pair<PerkType, PerkType>? = when (familyGroup()) {
		0 -> PerkType.STRONG_HOOK to PerkType.WISE_HOOK
		1 -> PerkType.GLIMMERING_HOOK to PerkType.PEARL_MAGNET
		2 -> PerkType.GREEDY_HOOK to PerkType.TREASURE_MAGNET
		3 -> PerkType.LUCKY_HOOK to PerkType.SPIRIT_MAGNET
		else -> null
	}

	fun pairSum(): Int {
		val types = pairTypes() ?: return 0
		return perkOrZero(types.first) + perkOrZero(types.second)
	}

	private fun perkOrZero(type: PerkType): Int = perkValue(type).coerceAtLeast(0)

	fun contentFingerprint(): String =
		"${kind.name}|${stability?.name}|${perks.joinToString(",") { "${it.type.name}:${it.value}" }}"

	fun stockDisplayRgb(): Int = stockRgb ?: stock?.rgb ?: 0xAAAAAA

	fun stabilityDisplayRgb(): Int = stabilityRgb ?: stability?.rgb ?: 0xAAAAAA

	fun rankNumber(): Int = if (rank > 0) rank else id

	fun displayTitle(): String = "${groupLabel()} #${rankNumber()}"

	fun guideLabel(): String = "${groupLabel()} #${rankNumber()}"

	fun markerRgb(): Int {
		pinColorOverride?.let { return it }
		if (kind == SpotKind.GROTTO) {
			return stabilityDisplayRgb()
		}
		val group = familyGroup()
		if (group != 0) return familyGroupRgb(group)
		val strong = perkValue(PerkType.STRONG_HOOK)
		if (strong >= 0) {
			val wise = perkValue(PerkType.WISE_HOOK).coerceAtLeast(0)
			val fish = perkValue(PerkType.FISH_MAGNET).coerceAtLeast(0)
			if (strong >= wise && strong >= fish) {
				return PerkFamily.STRONG.rgb
			}
		}
		return primaryPerk()?.type?.family?.rgb ?: PerkFamily.WISE.rgb
	}

	fun familyGroupRgb(group: Int = familyGroup()): Int = when (group) {
		0 -> PerkFamily.WISE.rgb
		1 -> PerkFamily.PEARL.rgb
		2 -> PerkFamily.TREASURE.rgb
		3 -> PerkFamily.SPIRIT.rgb
		4 -> PerkFamily.TICKET_WHITE.rgb
		5 -> PerkFamily.TICKET_RED.rgb
		6 -> PerkFamily.TICKET_BLUE.rgb
		7 -> PerkFamily.TICKET_PURPLE.rgb
		8 -> PerkFamily.TICKET_GREEN.rgb
		else -> 0xFFFFFF
	}
}
