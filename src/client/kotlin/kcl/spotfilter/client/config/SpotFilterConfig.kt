package kcl.spotfilter.client.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kcl.spotfilter.client.data.StockLevel
import kcl.spotfilter.client.filter.AutoPinRule
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterSlot
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.filter.SortDir
import kcl.spotfilter.client.filter.StockFilter
import kcl.spotfilter.client.parse.PerkType
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

class AutoPinRuleConfig {
	var name: String = "Rule"
	var enabled: Boolean = true
	var mode: String = FilterMode.AND.name
	var slot0: SlotConfig = SlotConfig()
	var slot1: SlotConfig = SlotConfig()
	var slot2: SlotConfig = SlotConfig()
	var stock: StockFilterConfig = StockFilterConfig()
	var customColorHex: String = ""
}

class SpotFilterConfig {
	var hudX: Int = 8
	var hudY: Int = 8
	var hudWidth: Int = 220
	var hudHeight: Int = 120
	var hudScale: Float = 1.0f
	var backgroundAlpha: Int = 40
	var hudVisible: Boolean = true
	var enabled: Boolean = true
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

	companion object {
		const val MIN_WIDTH = 120
		const val MIN_HEIGHT = 40
		const val MIN_SCALE = 0.5f
		const val MAX_SCALE = 3.0f

		private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
		private val path = FabricLoader.getInstance().configDir.resolve("spotfilter.json")

		val instance: SpotFilterConfig = load()

		fun load(): SpotFilterConfig {
			val loaded = if (Files.exists(path)) {
				Files.newBufferedReader(path).use { gson.fromJson(it, SpotFilterConfig::class.java) }
					?: SpotFilterConfig()
			} else {
				SpotFilterConfig()
			}
			loaded.clamp()
			loaded.applyToState()
			return loaded
		}

		fun save() {
			instance.syncFromState()
			instance.clamp()
			Files.createDirectories(path.parent)
			Files.newBufferedWriter(path).use { gson.toJson(instance, it) }
		}
	}

	fun applyToState() {
		FilterState.mode = runCatching { FilterMode.valueOf(filterMode) }.getOrDefault(FilterMode.AND)
		applySlot(FilterState.slots[0], slot0)
		applySlot(FilterState.slots[1], slot1)
		applySlot(FilterState.slots[2], slot2)
		applyStock(FilterState.stock, stock)
		FilterState.autoPinRules.clear()
		autoPinRules.forEach { FilterState.autoPinRules.add(fromConfig(it)) }
	}

	fun syncFromState() {
		filterMode = FilterState.mode.name
		slot0 = toSlotConfig(FilterState.slots[0])
		slot1 = toSlotConfig(FilterState.slots[1])
		slot2 = toSlotConfig(FilterState.slots[2])
		stock = toStockConfig(FilterState.stock)
		autoPinRules = FilterState.autoPinRules.map { toRuleConfig(it) }.toMutableList()
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

	private fun fromConfig(cfg: AutoPinRuleConfig): AutoPinRule {
		val rule = AutoPinRule()
		rule.name = cfg.name
		rule.enabled = cfg.enabled
		rule.mode = runCatching { FilterMode.valueOf(cfg.mode) }.getOrDefault(FilterMode.AND)
		applySlot(rule.slots[0], cfg.slot0)
		applySlot(rule.slots[1], cfg.slot1)
		applySlot(rule.slots[2], cfg.slot2)
		applyStock(rule.stock, cfg.stock)
		rule.customColorHex = cfg.customColorHex
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

	private fun toRuleConfig(rule: AutoPinRule): AutoPinRuleConfig {
		val cfg = AutoPinRuleConfig()
		cfg.name = rule.name
		cfg.enabled = rule.enabled
		cfg.mode = rule.mode.name
		cfg.slot0 = toSlotConfig(rule.slots[0])
		cfg.slot1 = toSlotConfig(rule.slots[1])
		cfg.slot2 = toSlotConfig(rule.slots[2])
		cfg.stock = toStockConfig(rule.stock)
		cfg.customColorHex = rule.customColorHex
		return cfg
	}
}
