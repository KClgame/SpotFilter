package kcl.spotfilter.client.config

import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.data.StabilityCost
import kcl.spotfilter.client.data.StockLevel
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterSlot
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.filter.PerkPairFilter
import kcl.spotfilter.client.filter.StabilityFilter
import kcl.spotfilter.client.filter.StockFilter
import kcl.spotfilter.client.parse.PerkType
import net.fabricmc.loader.api.FabricLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files

data class RulesLoadResult(
	val normal: List<AutoPinRule>,
	val grotto: List<AutoPinRule>,
	val errors: List<String>
)

object RulesFile {
	private val dir = FabricLoader.getInstance().configDir.resolve("spotfilter")
	val path = dir.resolve("rules.txt")
	var lastErrors: List<String> = emptyList()
		private set

	private val CONDITION = Regex(
		"""^(.+?)\s*(>=|<=|>|<|=|between|\.\.)\s*(.+)$""",
		RegexOption.IGNORE_CASE
	)

	fun load(): RulesLoadResult {
		if (!Files.exists(path)) {
			return RulesLoadResult(emptyList(), emptyList(), emptyList())
		}
		val text = Files.readString(path, StandardCharsets.UTF_8).removePrefix("\uFEFF")
		val result = parse(text)
		lastErrors = result.errors
		return result
	}

	fun exists(): Boolean = Files.exists(path)

	fun applyToState(result: RulesLoadResult) {
		FilterState.normal.autoPinRules.clear()
		FilterState.normal.autoPinRules.addAll(result.normal)
		FilterState.grotto.autoPinRules.clear()
		FilterState.grotto.autoPinRules.addAll(result.grotto)
	}

	fun saveFromState() {
		Files.createDirectories(dir)
		Files.writeString(path, format(FilterState.normal.autoPinRules, FilterState.grotto.autoPinRules), StandardCharsets.UTF_8)
	}

	fun writeTemplateIfMissing() {
		if (Files.exists(path)) return
		Files.createDirectories(dir)
		Files.writeString(path, TEMPLATE, StandardCharsets.UTF_8)
	}

	fun parse(text: String): RulesLoadResult {
		val normal = ArrayList<AutoPinRule>()
		val grotto = ArrayList<AutoPinRule>()
		val errors = ArrayList<String>()
		var kind = SpotKind.NORMAL
		var current: AutoPinRule? = null
		var nextSlot = 0

		fun flush() {
			val rule = current ?: return
			if (kind == SpotKind.GROTTO) grotto.add(rule) else normal.add(rule)
			current = null
			nextSlot = 0
		}

		fun startRule(): AutoPinRule {
			flush()
			val rule = AutoPinRule()
			current = rule
			nextSlot = 0
			return rule
		}

		fun ruleOrNew(): AutoPinRule = current ?: startRule()

		text.lineSequence().forEachIndexed { index, rawLine ->
			val lineNo = index + 1
			val line = rawLine.trim()
			if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
				if (line.isEmpty()) flush()
				return@forEachIndexed
			}
			val lower = line.lowercase()
			when {
				lower == "[normal]" || lower == "[island]" -> {
					flush()
					kind = SpotKind.NORMAL
				}
				lower == "[grotto]" -> {
					flush()
					kind = SpotKind.GROTTO
				}
				lower.startsWith("name=") -> {
					val rule = startRule()
					rule.name = line.substringAfter("=").trim().ifBlank { "Rule" }
				}
				lower.startsWith("nick=") || lower.startsWith("nickname=") -> {
					ruleOrNew().nickname = line.substringAfter("=").trim()
				}
				lower.startsWith("color=") || lower.startsWith("hex=") -> {
					ruleOrNew().customColorHex = line.substringAfter("=").trim()
				}
				lower.startsWith("mode=") -> {
					val value = line.substringAfter("=").trim()
					ruleOrNew().mode = runCatching { FilterMode.valueOf(value.uppercase()) }.getOrDefault(FilterMode.AND)
				}
				lower.startsWith("enabled=") -> {
					ruleOrNew().enabled = line.substringAfter("=").trim().toBooleanStrictOrNull() ?: true
				}
				lower.startsWith("f1=") || lower.startsWith("f2=") || lower.startsWith("f3=") -> {
					val slotIndex = line[1].digitToInt() - 1
					val expr = line.substringAfter("=").trim()
					val slot = parsePerkCondition(expr)
					if (slot == null && expr.isNotBlank()) {
						errors.add("line $lineNo: unknown perk condition '$expr'")
					} else if (slot != null) {
						ruleOrNew().slots[slotIndex].copyFrom(slot)
						nextSlot = maxOf(nextSlot, slotIndex + 1)
					}
				}
				lower.startsWith("stock=") || lower.startsWith("stock ") -> {
					val expr = if (lower.startsWith("stock=")) line.substringAfter("=").trim() else line.substringAfter("stock").trim()
					val stock = parseStock(expr)
					if (stock == null) errors.add("line $lineNo: bad stock '$expr'")
					else {
						val rule = ruleOrNew()
						rule.stock.enabled = stock.enabled
						rule.stock.compare = stock.compare
						rule.stock.level = stock.level
						rule.stock.levelMax = stock.levelMax
					}
				}
				lower.startsWith("pair=") || lower.startsWith("pair ") ||
					lower.startsWith("sum=") || lower.startsWith("sum ") -> {
					val expr = if (lower.startsWith("pair=") || lower.startsWith("sum=")) {
						line.substringAfter("=").trim()
					} else {
						line.substringAfter(' ').trim()
					}
					val pair = parsePair(expr)
					if (pair == null) errors.add("line $lineNo: bad pair '$expr'")
					else {
						val rule = ruleOrNew()
						rule.pair.enabled = pair.enabled
						rule.pair.compare = pair.compare
						rule.pair.threshold = pair.threshold
						rule.pair.thresholdMax = pair.thresholdMax
					}
				}
				lower.startsWith("cost=") || lower.startsWith("stability=") ||
					lower.startsWith("cost ") || lower.startsWith("stability ") -> {
					val expr = if (lower.startsWith("cost=") || lower.startsWith("stability=")) {
						line.substringAfter("=").trim()
					} else {
						line.substringAfter(' ').trim()
					}
					val cost = parseCost(expr)
					if (cost == null) errors.add("line $lineNo: bad cost '$expr'")
					else {
						val rule = ruleOrNew()
						rule.stability.enabled = cost.enabled
						rule.stability.compare = cost.compare
						rule.stability.level = cost.level
						rule.stability.levelMax = cost.levelMax
					}
				}
				else -> {
					val perk = parsePerkCondition(line)
					if (perk != null) {
						val rule = ruleOrNew()
						val slotIndex = (0..2).firstOrNull { !rule.slots[it].isActive } ?: 2
						rule.slots[slotIndex].copyFrom(perk)
					} else {
						errors.add("line $lineNo: cannot parse '$line'")
					}
				}
			}
		}
		flush()
		return RulesLoadResult(normal, grotto, errors)
	}

	fun format(normal: List<AutoPinRule>, grotto: List<AutoPinRule>): String = buildString {
		appendLine(HEADER)
		appendSection("normal", normal)
		appendLine()
		appendSection("grotto", grotto)
	}

	private fun StringBuilder.appendSection(title: String, rules: List<AutoPinRule>) {
		appendLine("[$title]")
		if (rules.isEmpty()) {
			appendLine("# (no rules)")
			return
		}
		rules.forEachIndexed { index, rule ->
			if (index > 0) appendLine()
			appendLine("name=${rule.name.ifBlank { "Rule ${index + 1}" }}")
			if (rule.nickname.isNotBlank()) appendLine("nick=${rule.nickname}")
			if (rule.customColorHex.isNotBlank()) appendLine("color=${rule.customColorHex}")
			appendLine("mode=${rule.mode.name}")
			appendLine("enabled=${rule.enabled}")
			rule.slots.forEachIndexed { slotIndex, slot ->
				if (slot.isActive) appendLine("f${slotIndex + 1}=${formatSlot(slot)}")
			}
			if (rule.stock.enabled) appendLine("stock=${formatStock(rule.stock)}")
			if (rule.pair.enabled) appendLine("pair=${formatPair(rule.pair)}")
			if (rule.stability.enabled) appendLine("cost=${formatCost(rule.stability)}")
		}
	}

	private fun formatSlot(slot: FilterSlot): String {
		val perk = slot.perk ?: return ""
		if (!perk.hasVariableValue) return perk.displayName
		return if (slot.compare == CompareOp.BETWEEN) {
			"${perk.displayName} between ${slot.threshold} ${slot.thresholdMax}"
		} else {
			"${perk.displayName} ${slot.compare.symbol} ${slot.threshold}"
		}
	}

	private fun formatStock(stock: StockFilter): String =
		if (stock.compare == CompareOp.BETWEEN) {
			"between ${stock.level.label} ${stock.levelMax.label}"
		} else {
			"${stock.compare.symbol} ${stock.level.label}"
		}

	private fun formatPair(pair: PerkPairFilter): String =
		if (pair.compare == CompareOp.BETWEEN) {
			"between ${pair.threshold} ${pair.thresholdMax}"
		} else {
			"${pair.compare.symbol} ${pair.threshold}"
		}

	private fun formatCost(cost: StabilityFilter): String =
		if (cost.compare == CompareOp.BETWEEN) {
			"between ${cost.level.label} ${cost.levelMax.label}"
		} else {
			"${cost.compare.symbol} ${cost.level.label}"
		}

	private fun parsePerkCondition(raw: String): FilterSlot? {
		val text = raw.trim()
		if (text.isEmpty()) return null
		val match = CONDITION.find(text)
		val slot = FilterSlot()
		if (match == null) {
			slot.perk = resolvePerk(text) ?: return null
			return slot
		}
		val perk = resolvePerk(match.groupValues[1]) ?: return null
		val compare = parseCompare(match.groupValues[2]) ?: return null
		slot.perk = perk
		slot.compare = compare
		if (!perk.hasVariableValue) return slot
		val rest = match.groupValues[3].trim()
		if (compare == CompareOp.BETWEEN) {
			val nums = rest.split(Regex("""[\s,;]+""")).mapNotNull { parseNumber(it) }
			if (nums.isEmpty()) return null
			slot.threshold = nums[0]
			slot.thresholdMax = nums.getOrElse(1) { nums[0] }
		} else {
			slot.threshold = parseNumber(rest) ?: return null
		}
		return slot
	}

	private fun parseStock(raw: String): StockFilter? {
		val text = raw.trim()
		if (text.isEmpty() || text.equals("off", true) || text.equals("none", true)) {
			return StockFilter()
		}
		val match = CONDITION.find("stock $text") ?: CONDITION.find(text)
		val filter = StockFilter()
		filter.enabled = true
		if (match == null) {
			filter.compare = CompareOp.EQ
			filter.level = resolveStock(text) ?: return null
			return filter
		}
		filter.compare = parseCompare(match.groupValues[2]) ?: return null
		val rest = match.groupValues[3].trim()
		if (filter.compare == CompareOp.BETWEEN) {
			val parts = rest.split(Regex("""[\s,;]+""")).filter { it.isNotBlank() }
			filter.level = resolveStock(parts.getOrNull(0) ?: return null) ?: return null
			filter.levelMax = resolveStock(parts.getOrNull(1) ?: parts[0]) ?: return null
		} else {
			filter.level = resolveStock(rest) ?: return null
		}
		return filter
	}

	private fun parsePair(raw: String): PerkPairFilter? {
		val text = raw.trim()
		if (text.isEmpty() || text.equals("off", true) || text.equals("none", true)) {
			return PerkPairFilter()
		}
		val match = CONDITION.find("pair $text") ?: CONDITION.find(text)
		val filter = PerkPairFilter()
		filter.enabled = true
		if (match == null) {
			filter.compare = CompareOp.EQ
			filter.threshold = parseNumber(text) ?: return null
			return filter
		}
		filter.compare = parseCompare(match.groupValues[2]) ?: return null
		val rest = match.groupValues[3].trim()
		if (filter.compare == CompareOp.BETWEEN) {
			val nums = rest.split(Regex("""[\s,;]+""")).mapNotNull { parseNumber(it) }
			if (nums.isEmpty()) return null
			filter.threshold = nums[0]
			filter.thresholdMax = nums.getOrElse(1) { nums[0] }
		} else {
			filter.threshold = parseNumber(rest) ?: return null
		}
		return filter
	}

	private fun parseCost(raw: String): StabilityFilter? {
		val text = raw.trim()
		if (text.isEmpty() || text.equals("off", true) || text.equals("none", true)) {
			return StabilityFilter()
		}
		val match = CONDITION.find("cost $text") ?: CONDITION.find(text)
		val filter = StabilityFilter()
		filter.enabled = true
		if (match == null) {
			filter.compare = CompareOp.EQ
			filter.level = resolveCost(text) ?: return null
			return filter
		}
		filter.compare = parseCompare(match.groupValues[2]) ?: return null
		val rest = match.groupValues[3].trim()
		if (filter.compare == CompareOp.BETWEEN) {
			val parts = rest.split(Regex("""[\s,;]+""")).filter { it.isNotBlank() }
			filter.level = resolveCost(parts.getOrNull(0) ?: return null) ?: return null
			filter.levelMax = resolveCost(parts.getOrNull(1) ?: parts[0]) ?: return null
		} else {
			filter.level = resolveCost(rest) ?: return null
		}
		return filter
	}

	private fun parseCompare(raw: String): CompareOp? = when (raw.trim().lowercase()) {
		">" -> CompareOp.GT
		">=" -> CompareOp.GTE
		"<" -> CompareOp.LT
		"<=" -> CompareOp.LTE
		"=", "==" -> CompareOp.EQ
		"between", ".." -> CompareOp.BETWEEN
		else -> null
	}

	private fun parseNumber(raw: String): Int? {
		val text = raw.trim().removePrefix("+").removeSuffix("%").trim()
		return text.toIntOrNull()
	}

	private fun resolvePerk(raw: String): PerkType? {
		val text = raw.trim().replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")
		if (text.isEmpty()) return null
		PerkType.entries.firstOrNull { it.displayName.equals(text, ignoreCase = true) }?.let { return it }
		PerkType.entries.firstOrNull { it.name.equals(text.replace(" ", "_"), ignoreCase = true) }?.let { return it }
		val compact = text.replace(" ", "")
		PerkType.entries.firstOrNull { it.displayName.replace(" ", "").equals(compact, ignoreCase = true) }?.let { return it }
		return ALIASES[compact.lowercase()]
	}

	private fun resolveStock(raw: String): StockLevel? {
		val text = raw.trim()
		StockLevel.fromLabel(text)?.let { return it }
		return runCatching { StockLevel.valueOf(text.uppercase().replace(' ', '_')) }.getOrNull()
	}

	private fun resolveCost(raw: String): StabilityCost? {
		val text = raw.trim()
		StabilityCost.entries.firstOrNull { it.label.equals(text, ignoreCase = true) }?.let { return it }
		return runCatching { StabilityCost.valueOf(text.uppercase()) }.getOrNull()
	}

	private val ALIASES = mapOf(
		"strong" to PerkType.STRONG_HOOK,
		"wise" to PerkType.WISE_HOOK,
		"glimmering" to PerkType.GLIMMERING_HOOK,
		"greedy" to PerkType.GREEDY_HOOK,
		"lucky" to PerkType.LUCKY_HOOK,
		"xp" to PerkType.XP_MAGNET,
		"fishmagnet" to PerkType.FISH_MAGNET,
		"pearlmagnet" to PerkType.PEARL_MAGNET,
		"treasuremagnet" to PerkType.TREASURE_MAGNET,
		"spiritmagnet" to PerkType.SPIRIT_MAGNET,
		"elusive" to PerkType.ELUSIVE_CHANCE,
		"wayfinder" to PerkType.WAYFINDER_DATA,
		"fishchance" to PerkType.FISH_CHANCE,
		"pearlchance" to PerkType.PEARL_CHANCE,
		"treasurechance" to PerkType.TREASURE_CHANCE,
		"spiritchance" to PerkType.SPIRIT_CHANCE
	)

	private val HEADER = """
		|# SpotFilter Auto Pin rules
		|# Path: config/spotfilter/rules.txt
		|# Reload in-game: /sf reload   or   /sf rules reload
		|# Docs: README.md  (Auto Pin 与 rules.txt)
		|#
		|# [normal]  island spots
		|# [grotto]  Stability Cost spots
		|#
		|# Each rule starts with name=
		|#   nick=HUD / world-guide group name  (Name #1, #2…)
		|#   color=#RRGGBB
		|#   mode=AND|OR
		|#   enabled=true|false
		|#   f1=Strong Hook >= 20
		|#   f2=Fish Magnet = 200
		|#   f3=Pearl Chance
		|#   f1=Fish Magnet between 10 30
		|#   stock >= High
		|#   stock between Medium Plentiful
		|#   pair >= 40
		|#   pair between 30 60
		|#   cost <= Medium
		|#
		|# Blank line ends a rule. Lines starting with # or // are comments.
		|
	""".trimMargin()

	private val TEMPLATE = HEADER + """
		|[normal]
		|# name=Example
		|# nick=Big Fish
		|# f1=Strong Hook >= 20
		|# f2=Wise Hook >= 20
		|# stock >= High
		|
		|[grotto]
		|# name=Cheap Pearl
		|# nick=珍珠
		|# f1=Glimmering Hook >= 20
		|# cost <= Medium
		|
	""".trimMargin()
}
