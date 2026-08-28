package kcl.spotfilter.client.parse

import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotKey
import kcl.spotfilter.client.data.StockLevel
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Display
import net.minecraft.world.level.Level
import java.util.Optional

object SpotParser {
	private val STOCK = Regex(
		"""Stock:\s*(Plentiful|Very High|High|Medium|Low)""",
		RegexOption.IGNORE_CASE
	)
	private val HOOK = Regex(
		"""\+?\s*(10|20|30)\s*%\s*(Strong|Wise|Glimmering|Greedy|Lucky)\s+Hook""",
		RegexOption.IGNORE_CASE
	)
	private val MAGNET = Regex(
		"""\+?\s*(10|20|30)\s*%\s*(XP|Fish|Pearl|Treasure|Spirit)\s+Magnet""",
		RegexOption.IGNORE_CASE
	)
	private val ELUSIVE = Regex("""\+?\s*5\s*%\s*Elusive\s+Chance""", RegexOption.IGNORE_CASE)
	private val WAYFINDER = Regex("""\+?\s*10\s+Wayfinder\s+Data""", RegexOption.IGNORE_CASE)
	private val PEARL_CHANCE = Regex("""\+?\s*5\s*%\s*Pearl\s+Chance""", RegexOption.IGNORE_CASE)
	private val TREASURE_CHANCE = Regex("""\+?\s*1\s*%\s*Treasure\s+Chance""", RegexOption.IGNORE_CASE)
	private val SPIRIT_CHANCE = Regex("""\+?\s*2\s*%\s*Spirit\s+Chance""", RegexOption.IGNORE_CASE)

	data class StyledSpan(val text: String, val rgb: Int?)

	fun flatten(component: Component): String = component.string

	fun spans(component: Component): List<StyledSpan> {
		val out = ArrayList<StyledSpan>()
		component.visit({ style: Style, text: String ->
			if (text.isNotEmpty()) {
				out.add(StyledSpan(text, style.color?.value))
			}
			Optional.empty<Any>()
		}, Style.EMPTY)
		return out
	}

	fun colorContaining(spans: List<StyledSpan>, needle: String): Int? {
		if (needle.isBlank()) return null
		return spans.firstOrNull { span ->
			span.rgb != null && span.text.contains(needle, ignoreCase = true)
		}?.rgb
	}

	fun isFishingSpot(text: String): Boolean =
		text.contains("Fishing Spot", ignoreCase = true)

	fun parse(
		level: Level,
		entity: Display.TextDisplay,
		component: Component,
		now: Long
	): FishingSpot? {
		val text = flatten(component)
		if (!isFishingSpot(text)) return null
		val styled = spans(component)
		val pos = entity.blockPosition()
		val perks = parsePerks(text, styled).take(3)
		val stock = STOCK.find(text)?.groupValues?.get(1)?.let { StockLevel.fromLabel(it) }
		return FishingSpot(
			key = SpotKey(level.dimension().identifier(), pos.x, pos.y, pos.z),
			entityId = entity.id,
			x = pos.x,
			y = pos.y,
			z = pos.z,
			stock = stock,
			stockRgb = stock?.let { colorContaining(styled, it.label) },
			perks = perks,
			lastSeenGameTime = now
		)
	}

	fun parsePerks(text: String, styled: List<StyledSpan> = emptyList()): List<ParsedPerk> {
		val found = ArrayList<ParsedPerk>(3)
		HOOK.findAll(text).forEach { match ->
			val value = match.groupValues[1].toInt()
			val type = when (match.groupValues[2].lowercase()) {
				"strong" -> PerkType.STRONG_HOOK
				"wise" -> PerkType.WISE_HOOK
				"glimmering" -> PerkType.GLIMMERING_HOOK
				"greedy" -> PerkType.GREEDY_HOOK
				"lucky" -> PerkType.LUCKY_HOOK
				else -> return@forEach
			}
			found.add(colored(type, value, styled))
		}
		MAGNET.findAll(text).forEach { match ->
			val value = match.groupValues[1].toInt()
			val type = when (match.groupValues[2].lowercase()) {
				"xp" -> PerkType.XP_MAGNET
				"fish" -> PerkType.FISH_MAGNET
				"pearl" -> PerkType.PEARL_MAGNET
				"treasure" -> PerkType.TREASURE_MAGNET
				"spirit" -> PerkType.SPIRIT_MAGNET
				else -> return@forEach
			}
			found.add(colored(type, value, styled))
		}
		if (ELUSIVE.containsMatchIn(text)) found.add(colored(PerkType.ELUSIVE_CHANCE, 5, styled))
		if (WAYFINDER.containsMatchIn(text)) found.add(colored(PerkType.WAYFINDER_DATA, 10, styled))
		if (PEARL_CHANCE.containsMatchIn(text)) found.add(colored(PerkType.PEARL_CHANCE, 5, styled))
		if (TREASURE_CHANCE.containsMatchIn(text)) found.add(colored(PerkType.TREASURE_CHANCE, 1, styled))
		if (SPIRIT_CHANCE.containsMatchIn(text)) found.add(colored(PerkType.SPIRIT_CHANCE, 2, styled))
		return found.distinctBy { it.type }
	}

	private fun colored(type: PerkType, value: Int, styled: List<StyledSpan>): ParsedPerk =
		ParsedPerk(
			type = type,
			value = value,
			nameRgb = colorContaining(styled, type.displayName),
			valueRgb = colorContaining(styled, type.valueLabel(value))
		)

	fun keyAt(dimension: Identifier, pos: BlockPos): SpotKey =
		SpotKey(dimension, pos.x, pos.y, pos.z)
}
