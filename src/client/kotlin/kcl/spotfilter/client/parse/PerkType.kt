package kcl.spotfilter.client.parse

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.data.SpotKind
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier

enum class PerkFamily(val rgb: Int) {
	STRONG(0xFC5454),
	WISE(0x2199F0),
	PEARL(0x8636FF),
	TREASURE(0xFC7D3F),
	SPIRIT(0x23C525);

	fun textColor(): TextColor = TextColor.fromRgb(rgb)

	fun nameStyle(): Style = Style.EMPTY.withColor(textColor())
}

enum class PerkKind {
	HOOK,
	MAGNET,
	SPECIAL
}

enum class PerkType(
	val displayName: String,
	val family: PerkFamily,
	val kind: PerkKind,
	val percent: Boolean
) {
	STRONG_HOOK("Strong Hook", PerkFamily.STRONG, PerkKind.HOOK, true),
	WISE_HOOK("Wise Hook", PerkFamily.WISE, PerkKind.HOOK, true),
	GLIMMERING_HOOK("Glimmering Hook", PerkFamily.PEARL, PerkKind.HOOK, true),
	GREEDY_HOOK("Greedy Hook", PerkFamily.TREASURE, PerkKind.HOOK, true),
	LUCKY_HOOK("Lucky Hook", PerkFamily.SPIRIT, PerkKind.HOOK, true),

	XP_MAGNET("XP Magnet", PerkFamily.STRONG, PerkKind.MAGNET, true),
	FISH_MAGNET("Fish Magnet", PerkFamily.WISE, PerkKind.MAGNET, true),
	PEARL_MAGNET("Pearl Magnet", PerkFamily.PEARL, PerkKind.MAGNET, true),
	TREASURE_MAGNET("Treasure Magnet", PerkFamily.TREASURE, PerkKind.MAGNET, true),
	SPIRIT_MAGNET("Spirit Magnet", PerkFamily.SPIRIT, PerkKind.MAGNET, true),

	ELUSIVE_CHANCE("Elusive Chance", PerkFamily.STRONG, PerkKind.SPECIAL, true),
	WAYFINDER_DATA("Wayfinder Data", PerkFamily.WISE, PerkKind.SPECIAL, false),
	FISH_CHANCE("Fish Chance", PerkFamily.WISE, PerkKind.SPECIAL, true),
	PEARL_CHANCE("Pearl Chance", PerkFamily.PEARL, PerkKind.SPECIAL, true),
	TREASURE_CHANCE("Treasure Chance", PerkFamily.TREASURE, PerkKind.SPECIAL, true),
	SPIRIT_CHANCE("Spirit Chance", PerkFamily.SPIRIT, PerkKind.SPECIAL, true);

	val isGrottoChance: Boolean
		get() = this == FISH_CHANCE || this == PEARL_CHANCE ||
			this == TREASURE_CHANCE || this == SPIRIT_CHANCE

	val skipsSpotColor: Boolean
		get() = this == XP_MAGNET || this == WAYFINDER_DATA

	/** fish=0, pearl=1, treasure=2, spirit=3; null = does not group a spot. */
	val groupingIndex: Int?
		get() = when (this) {
			STRONG_HOOK, WISE_HOOK, FISH_MAGNET, FISH_CHANCE, ELUSIVE_CHANCE -> 0
			GLIMMERING_HOOK, PEARL_MAGNET, PEARL_CHANCE -> 1
			GREEDY_HOOK, TREASURE_MAGNET, TREASURE_CHANCE -> 2
			LUCKY_HOOK, SPIRIT_MAGNET, SPIRIT_CHANCE -> 3
			else -> null
		}

	val textureId: Identifier
		get() = Identifier.fromNamespaceAndPath(
			SpotFilter.MOD_ID,
			"textures/gui/perk/${if (this == FISH_CHANCE) "wayfinder_data" else name.lowercase()}.png"
		)

	val hasVariableValue: Boolean
		get() = kind == PerkKind.HOOK || kind == PerkKind.MAGNET

	fun allowedIn(spotKind: SpotKind): Boolean = when (spotKind) {
		SpotKind.NORMAL -> this != FISH_CHANCE
		SpotKind.GROTTO -> this != WAYFINDER_DATA
	}

	fun valueLabel(value: Int): String =
		if (percent) "+$value%" else "+$value"

	fun coloredLine(value: Int): Component =
		Component.literal(valueLabel(value) + " ")
			.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)))
			.append(Component.literal(displayName).withStyle(family.nameStyle()))
}

data class ParsedPerk(
	val type: PerkType,
	val value: Int,
	val nameRgb: Int? = null,
	val valueRgb: Int? = null
) {
	fun coloredLine(): Component {
		val valueColor = valueRgb ?: 0xFFFFFF
		val nameColor = nameRgb ?: type.family.rgb
		return Component.literal(type.valueLabel(value) + " ")
			.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(valueColor)))
			.append(Component.literal(type.displayName).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(nameColor))))
	}

	fun resolvedNameRgb(): Int = nameRgb ?: type.family.rgb
}

object PerkPriority {
	fun primary(perks: List<ParsedPerk>): ParsedPerk? {
		if (perks.isEmpty()) return null
		val special = perks.filter { it.type.kind == PerkKind.SPECIAL }
		val pool = special.ifEmpty { perks }
		return pool.maxWithOrNull(compareBy<ParsedPerk> { it.value }
			.thenBy { if (it.type == PerkType.STRONG_HOOK) 1 else 0 })
	}

	fun grottoChance(perks: List<ParsedPerk>): ParsedPerk? =
		perks.firstOrNull { it.type.isGrottoChance }

	fun grottoDisplay(perks: List<ParsedPerk>): ParsedPerk? {
		val bonuses = perks.filter { !it.type.isGrottoChance && !it.type.skipsSpotColor }
		val best = bonuses.maxWithOrNull(
			compareBy<ParsedPerk> { it.value }
				.thenBy {
					when (it.type.kind) {
						PerkKind.SPECIAL -> 2
						PerkKind.MAGNET -> 1
						PerkKind.HOOK -> 0
					}
				}
		)
		return best ?: grottoChance(perks)
	}
}
