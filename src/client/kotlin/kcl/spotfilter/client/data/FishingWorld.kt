package kcl.spotfilter.client.data

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.filter.FilterState
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot

enum class FishingClimate(val label: String) {
	TEMPERATE("Temperate"),
	TROPICAL("Tropical"),
	BARREN("Barren")
}

enum class FishingPlace(
	val id: Int,
	val displayName: String,
	val shortId: String,
	val kind: SpotKind,
	val climate: FishingClimate
) {
	I1(1, "Verdant Woods", "I1", SpotKind.NORMAL, FishingClimate.TEMPERATE),
	I2(2, "Floral Forest", "I2", SpotKind.NORMAL, FishingClimate.TEMPERATE),
	I3(3, "Dark Grove", "I3", SpotKind.NORMAL, FishingClimate.TEMPERATE),
	I4(4, "Tropical Overgrowth", "I4", SpotKind.NORMAL, FishingClimate.TROPICAL),
	I5(5, "Coral Shores", "I5", SpotKind.NORMAL, FishingClimate.TROPICAL),
	I6(6, "Twisted Swamp", "I6", SpotKind.NORMAL, FishingClimate.TROPICAL),
	I7(7, "Ancient Sands", "I7", SpotKind.NORMAL, FishingClimate.BARREN),
	I8(8, "Blazing Canyon", "I8", SpotKind.NORMAL, FishingClimate.BARREN),
	I9(9, "Ashen Wastes", "I9", SpotKind.NORMAL, FishingClimate.BARREN),
	GROTTO_TEMPERATE(10, "Sunken Swamp", "Temperate", SpotKind.GROTTO, FishingClimate.TEMPERATE),
	GROTTO_TROPICAL(11, "Mirrored Oasis", "Tropical", SpotKind.GROTTO, FishingClimate.TROPICAL),
	GROTTO_BARREN(12, "Volcanic Springs", "Barren", SpotKind.GROTTO, FishingClimate.BARREN);

	companion object {
		fun fromScoreText(raw: String): FishingPlace? {
			val cleaned = raw.replace(Regex("§."), "").trim()
			val named = Regex("""MCCI:\s*(.+)""", RegexOption.IGNORE_CASE)
				.find(cleaned)
				?.groupValues
				?.get(1)
				?.trim()
				?: cleaned
			return entries.firstOrNull { named.equals(it.displayName, ignoreCase = true) }
				?: entries.firstOrNull { cleaned.contains(it.displayName, ignoreCase = true) }
		}
	}
}

object FishingWorld {
	var current: FishingPlace? = null
		private set
	private var lastWorldKey: String? = null
	private var enabledManual: Boolean? = null

	fun overlayOn(): Boolean = SpotFilterConfig.instance.enabled

	fun toggleManual() {
		setManual(!overlayOn())
	}

	fun setManual(enabled: Boolean) {
		enabledManual = enabled
		applyEnabled(enabled)
		SpotFilterConfig.save()
	}

	fun tick(client: Minecraft) {
		val level = client.level
		if (level == null || client.player == null) {
			if (current != null) {
				current = null
				SpotPool.clearSpots()
			}
			if (enabledManual == null) applyEnabled(false)
			return
		}
		val worldKey = level.dimension().identifier().toString()
		if (worldKey != lastWorldKey) {
			lastWorldKey = worldKey
			enabledManual = null
		}
		val next = detectPlace(client)
		val prev = current
		if (prev != next) {
			current = next
			if (prev != null) {
				SpotPool.clearSpots()
			}
			if (next != null) {
				FilterState.kind = next.kind
			}
		}
		if (enabledManual == null) {
			applyEnabled(autoEnabled(client))
		}
	}

	fun autoEnabled(client: Minecraft = Minecraft.getInstance()): Boolean {
		if (client.level == null) return false
		if (worldIdHasFishing(client)) return true
		return current != null
	}

	fun worldIdHasFishing(client: Minecraft): Boolean {
		val level = client.level ?: return false
		return level.dimension().identifier().toString().contains("fishing", ignoreCase = true)
	}

	private fun applyEnabled(want: Boolean) {
		val cfg = SpotFilterConfig.instance
		if (cfg.enabled == want) return
		cfg.enabled = want
		if (!want) PinnedSpotMarker.removeAll()
	}

	private fun detectPlace(client: Minecraft): FishingPlace? {
		val level = client.level ?: return null
		val objective = level.scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
		return FishingPlace.fromScoreText(objective.displayName.string)
	}
}
