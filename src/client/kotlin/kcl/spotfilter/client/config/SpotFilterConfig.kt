package kcl.spotfilter.client.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kcl.spotfilter.client.data.SpotKind
import kcl.spotfilter.client.data.StabilityCost
import kcl.spotfilter.client.data.StockLevel
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterProfile
import kcl.spotfilter.client.filter.FilterSlot
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.filter.SortDir
import kcl.spotfilter.client.filter.PerkPairFilter
import kcl.spotfilter.client.filter.StabilityFilter
import kcl.spotfilter.client.filter.StockFilter
import kcl.spotfilter.client.parse.PerkType
import kcl.spotfilter.client.ui.HudLayout
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

class SlotConfig {
	var perk: String? = null
	var compare: String = CompareOp.GT.name
	var threshold: Int = 10
	var thresholdMax: Int = 30
	var sortDir: String = SortDir.DESC.name
}

class StockFilterConfig {
	var enabled: Boolean = false
	var compare: String = CompareOp.GT.name
	var level: String = StockLevel.HIGH.name
	var levelMax: String = StockLevel.PLENTIFUL.name
}

class StabilityFilterConfig {
	var enabled: Boolean = false
	var compare: String = CompareOp.EQ.name
	var level: String = StabilityCost.LOW.name
	var levelMax: String = StabilityCost.HIGH.name
}

class PairFilterConfig {
	var enabled: Boolean = false
	var compare: String = CompareOp.GTE.name
	var threshold: Int = 40
	var thresholdMax: Int = 60
}

class AutoPinRuleConfig {
	var name: String = "Rule"
	var enabled: Boolean = true
	var mode: String = FilterMode.AND.name
	var slot0: SlotConfig = SlotConfig()
	var slot1: SlotConfig = SlotConfig()
	var slot2: SlotConfig = SlotConfig()
	var stock: StockFilterConfig = StockFilterConfig()
	var stability: StabilityFilterConfig = StabilityFilterConfig()
	var pair: PairFilterConfig = PairFilterConfig()
	var customColorHex: String = ""
	var nickname: String = ""
}

class FilterProfileConfig {
	var filterMode: String = FilterMode.AND.name
	var slot0: SlotConfig = SlotConfig()
	var slot1: SlotConfig = SlotConfig()
	var slot2: SlotConfig = SlotConfig()
	var stock: StockFilterConfig = StockFilterConfig()
	var stability: StabilityFilterConfig = StabilityFilterConfig()
	var pair: PairFilterConfig = PairFilterConfig()
	var autoPinRules: MutableList<AutoPinRuleConfig> = ArrayList()
}

class SpotFilterConfig {
	var hudX: Int = 8
	var hudY: Int = 8
	var hudWidth: Int = 220
	var hudHeight: Int = 120
	var hudScale: Float = 1.0f
	var backgroundAlpha: Int = 40
	var hudVisible: Boolean = true
	var hudLayout: String = HudLayout.DETAILED.name
	var kickDepleted: Boolean = true
	var enabled: Boolean = true
	var spotKind: String = SpotKind.NORMAL.name
	var normal: FilterProfileConfig = FilterProfileConfig()
	var grotto: FilterProfileConfig = FilterProfileConfig()

	// Pre-1.0.6 fields kept so old configs still load.
	var filterMode: String = FilterMode.AND.name
	var slot0: SlotConfig = SlotConfig()
	var slot1: SlotConfig = SlotConfig()
	var slot2: SlotConfig = SlotConfig()
	var stock: StockFilterConfig = StockFilterConfig()
	var autoPinRules: MutableList<AutoPinRuleConfig> = ArrayList()

	fun clamp() {
		hudWidth = hudWidth.coerceIn(MIN_WIDTH, 480)
		hudHeight = hudHeight.coerceIn(MIN_HEIGHT, 360)
		hudScale = hudScale.coerceIn(MIN_SCALE, MAX_SCALE)
		backgroundAlpha = backgroundAlpha.coerceIn(0, 90)
		hudX = hudX.coerceAtLeast(0)
		hudY = hudY.coerceAtLeast(0)
	}

	fun layout(): HudLayout = HudLayout.fromName(hudLayout)

	fun setLayout(layout: HudLayout) {
		hudLayout = layout.name
	}

	companion object {
		const val MIN_WIDTH = 120
		const val MIN_HEIGHT = 40
		const val MIN_SCALE = 0.5f
		const val MAX_SCALE = 3.0f

		private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
		private val path = FabricLoader.getInstance().configDir.resolve("spotfilter.json")

		val instance: SpotFilterConfig = load()

		fun load(): SpotFilterConfig {
			val loaded = readFile()
			loaded.applyToState()
			loadRules()
			return loaded
		}

		fun reload() {
			val loaded = readFile()
			instance.copyFrom(loaded)
			instance.applyToState()
			loadRules()
		}

		fun save() {
			instance.syncFromState()
			instance.clamp()
			Files.createDirectories(path.parent)
			Files.newBufferedWriter(path).use { gson.toJson(instance, it) }
			RulesFile.saveFromState()
		}

		private fun loadRules() {
			if (RulesFile.exists()) {
				RulesFile.applyToState(RulesFile.load())
			} else if (FilterState.normal.autoPinRules.isNotEmpty() || FilterState.grotto.autoPinRules.isNotEmpty()) {
				RulesFile.saveFromState()
			} else {
				RulesFile.writeTemplateIfMissing()
			}
		}

		private fun readFile(): SpotFilterConfig {
			val loaded = if (Files.exists(path)) {
				Files.newBufferedReader(path).use { gson.fromJson(it, SpotFilterConfig::class.java) }
					?: SpotFilterConfig()
			} else {
				SpotFilterConfig()
			}
			loaded.clamp()
			loaded.migrateLegacy()
			return loaded
		}
	}

	fun copyFrom(other: SpotFilterConfig) {
		hudX = other.hudX
		hudY = other.hudY
		hudWidth = other.hudWidth
		hudHeight = other.hudHeight
		hudScale = other.hudScale
		backgroundAlpha = other.backgroundAlpha
		hudVisible = other.hudVisible
		hudLayout = other.hudLayout
		kickDepleted = other.kickDepleted
		enabled = other.enabled
		spotKind = other.spotKind
		normal = other.normal
		grotto = other.grotto
		filterMode = other.filterMode
		slot0 = other.slot0
		slot1 = other.slot1
		slot2 = other.slot2
		stock = other.stock
		autoPinRules = other.autoPinRules
	}

	private fun migrateLegacy() {
		val legacyActive = slot0.perk != null || slot1.perk != null || slot2.perk != null ||
			stock.enabled || autoPinRules.isNotEmpty()
		val nestedEmpty = normal.slot0.perk == null && normal.slot1.perk == null &&
			normal.slot2.perk == null && !normal.stock.enabled && normal.autoPinRules.isEmpty()
		if (legacyActive && nestedEmpty) {
			normal.filterMode = filterMode
			normal.slot0 = slot0
			normal.slot1 = slot1
			normal.slot2 = slot2
			normal.stock = stock
			normal.autoPinRules = autoPinRules
		}
	}

	fun applyToState() {
		FilterState.kind = runCatching { SpotKind.valueOf(spotKind) }.getOrDefault(SpotKind.NORMAL)
		applyProfile(FilterState.normal, normal)
		applyProfile(FilterState.grotto, grotto)
	}

	fun syncFromState() {
		spotKind = FilterState.kind.name
		normal = toProfileConfig(FilterState.normal)
		grotto = toProfileConfig(FilterState.grotto)
		filterMode = FilterState.normal.mode.name
		slot0 = toSlotConfig(FilterState.normal.slots[0])
		slot1 = toSlotConfig(FilterState.normal.slots[1])
		slot2 = toSlotConfig(FilterState.normal.slots[2])
		stock = toStockConfig(FilterState.normal.stock)
		autoPinRules = FilterState.normal.autoPinRules.map { toRuleConfig(it) }.toMutableList()
	}

	private fun applyProfile(target: FilterProfile, cfg: FilterProfileConfig?) {
		if (cfg == null) return
		target.mode = runCatching { FilterMode.valueOf(cfg.filterMode) }.getOrDefault(FilterMode.AND)
		applySlot(target.slots[0], cfg.slot0)
		applySlot(target.slots[1], cfg.slot1)
		applySlot(target.slots[2], cfg.slot2)
		applyStock(target.stock, cfg.stock)
		applyStability(target.stability, cfg.stability)
		applyPair(target.pair, cfg.pair)
		target.autoPinRules.clear()
		cfg.autoPinRules.forEach { target.autoPinRules.add(fromConfig(it)) }
	}

	private fun toProfileConfig(profile: FilterProfile): FilterProfileConfig {
		val cfg = FilterProfileConfig()
		cfg.filterMode = profile.mode.name
		cfg.slot0 = toSlotConfig(profile.slots[0])
		cfg.slot1 = toSlotConfig(profile.slots[1])
		cfg.slot2 = toSlotConfig(profile.slots[2])
		cfg.stock = toStockConfig(profile.stock)
		cfg.stability = toStabilityConfig(profile.stability)
		cfg.pair = toPairConfig(profile.pair)
		cfg.autoPinRules = profile.autoPinRules.map { toRuleConfig(it) }.toMutableList()
		return cfg
	}

	private fun applySlot(slot: FilterSlot, cfg: SlotConfig?) {
		slot.clear()
		if (cfg == null) return
		slot.perk = cfg.perk?.let { runCatching { PerkType.valueOf(it) }.getOrNull() }
		slot.compare = runCatching { CompareOp.valueOf(cfg.compare) }.getOrDefault(CompareOp.GT)
		slot.threshold = cfg.threshold
		slot.thresholdMax = cfg.thresholdMax
		slot.sortDir = runCatching { SortDir.valueOf(cfg.sortDir) }.getOrDefault(SortDir.DESC)
	}

	private fun applyStock(target: StockFilter, cfg: StockFilterConfig?) {
		if (cfg == null) return
		target.enabled = cfg.enabled
		target.compare = runCatching { CompareOp.valueOf(cfg.compare) }.getOrDefault(CompareOp.GT)
		target.level = runCatching { StockLevel.valueOf(cfg.level) }.getOrDefault(StockLevel.HIGH)
		target.levelMax = runCatching { StockLevel.valueOf(cfg.levelMax) }.getOrDefault(StockLevel.PLENTIFUL)
	}

	private fun applyStability(target: StabilityFilter, cfg: StabilityFilterConfig?) {
		if (cfg == null) return
		target.enabled = cfg.enabled
		target.compare = runCatching { CompareOp.valueOf(cfg.compare) }.getOrDefault(CompareOp.EQ)
		target.level = runCatching { StabilityCost.valueOf(cfg.level) }.getOrDefault(StabilityCost.LOW)
		target.levelMax = runCatching { StabilityCost.valueOf(cfg.levelMax) }.getOrDefault(StabilityCost.HIGH)
	}

	private fun applyPair(target: PerkPairFilter, cfg: PairFilterConfig?) {
		if (cfg == null) return
		target.enabled = cfg.enabled
		target.compare = runCatching { CompareOp.valueOf(cfg.compare) }.getOrDefault(CompareOp.GTE)
		target.threshold = cfg.threshold.coerceIn(10, 60)
		target.thresholdMax = cfg.thresholdMax.coerceIn(10, 60)
	}

	private fun fromConfig(cfg: AutoPinRuleConfig): AutoPinRule {
		val rule = AutoPinRule()
		rule.name = cfg.name
		rule.enabled = cfg.enabled
		rule.mode = runCatching { FilterMode.valueOf(cfg.mode) }.getOrDefault(FilterMode.AND)
		applySlot(rule.slots[0], cfg.slot0)
		applySlot(rule.slots[1], cfg.slot1)
		applySlot(rule.slots[2], cfg.slot2)
		applyStock(rule.stock, cfg.stock)
		applyStability(rule.stability, cfg.stability)
		applyPair(rule.pair, cfg.pair)
		rule.customColorHex = cfg.customColorHex
		rule.nickname = cfg.nickname
		return rule
	}

	private fun toSlotConfig(slot: FilterSlot): SlotConfig {
		val cfg = SlotConfig()
		cfg.perk = slot.perk?.name
		cfg.compare = slot.compare.name
		cfg.threshold = slot.threshold
		cfg.thresholdMax = slot.thresholdMax
		cfg.sortDir = slot.sortDir.name
		return cfg
	}

	private fun toStockConfig(stock: StockFilter): StockFilterConfig {
		val cfg = StockFilterConfig()
		cfg.enabled = stock.enabled
		cfg.compare = stock.compare.name
		cfg.level = stock.level.name
		cfg.levelMax = stock.levelMax.name
		return cfg
	}

	private fun toStabilityConfig(stability: StabilityFilter): StabilityFilterConfig {
		val cfg = StabilityFilterConfig()
		cfg.enabled = stability.enabled
		cfg.compare = stability.compare.name
		cfg.level = stability.level.name
		cfg.levelMax = stability.levelMax.name
		return cfg
	}

	private fun toPairConfig(pair: PerkPairFilter): PairFilterConfig {
		val cfg = PairFilterConfig()
		cfg.enabled = pair.enabled
		cfg.compare = pair.compare.name
		cfg.threshold = pair.threshold
		cfg.thresholdMax = pair.thresholdMax
		return cfg
	}

	private fun toRuleConfig(rule: AutoPinRule): AutoPinRuleConfig {
		val cfg = AutoPinRuleConfig()
		cfg.name = rule.name
		cfg.enabled = rule.enabled
		cfg.mode = rule.mode.name
		cfg.slot0 = toSlotConfig(rule.slots[0])
		cfg.slot1 = toSlotConfig(rule.slots[1])
		cfg.slot2 = toSlotConfig(rule.slots[2])
		cfg.stock = toStockConfig(rule.stock)
		cfg.stability = toStabilityConfig(rule.stability)
		cfg.pair = toPairConfig(rule.pair)
		cfg.customColorHex = rule.customColorHex
		cfg.nickname = rule.nickname
		return cfg
	}
}
