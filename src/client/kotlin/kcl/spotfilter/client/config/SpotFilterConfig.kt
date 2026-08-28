package kcl.spotfilter.client.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kcl.spotfilter.client.filter.CompareOp
import kcl.spotfilter.client.filter.FilterMode
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.filter.SortDir
import kcl.spotfilter.client.parse.PerkType
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

class SlotConfig {
	var perk: String? = null
	var compare: String = CompareOp.GT.name
	var threshold: Int = 10
	var sortDir: String = SortDir.DESC.name
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
		applySlot(0, slot0)
		applySlot(1, slot1)
		applySlot(2, slot2)
	}

	fun syncFromState() {
		filterMode = FilterState.mode.name
		slot0 = toConfig(FilterState.slots[0])
		slot1 = toConfig(FilterState.slots[1])
		slot2 = toConfig(FilterState.slots[2])
	}

	private fun applySlot(index: Int, cfg: SlotConfig?) {
		val slot = FilterState.slots[index]
		if (cfg == null) {
			slot.clear()
			return
		}
		slot.perk = cfg.perk?.let { runCatching { PerkType.valueOf(it) }.getOrNull() }
		slot.compare = runCatching { CompareOp.valueOf(cfg.compare) }.getOrDefault(CompareOp.GT)
		slot.threshold = cfg.threshold
		slot.sortDir = runCatching { SortDir.valueOf(cfg.sortDir) }.getOrDefault(SortDir.DESC)
	}

	private fun toConfig(slot: kcl.spotfilter.client.filter.FilterSlot): SlotConfig {
		val cfg = SlotConfig()
		cfg.perk = slot.perk?.name
		cfg.compare = slot.compare.name
		cfg.threshold = slot.threshold
		cfg.sortDir = slot.sortDir.name
		return cfg
	}
}
