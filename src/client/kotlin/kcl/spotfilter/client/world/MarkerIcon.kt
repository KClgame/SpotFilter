package kcl.spotfilter.client.world

import kcl.spotfilter.SpotFilter
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.filter.AutoPin
import kcl.spotfilter.client.parse.PerkType
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.random.Random

object MarkerIcon {
	private val FISH: Array<Item> = arrayOf(
		Items.COD,
		Items.SALMON,
		Items.PUFFERFISH,
		Items.TROPICAL_FISH
	)
	private val SPIRIT_FAMILIES = arrayOf("strong", "wise", "glimmering", "greedy", "lucky")

	fun stackFor(spot: FishingSpot): ItemStack {
		val family = spot.familyGroup()
		val pair = spot.pairSum()
		if (pair == 60) {
			return when (family) {
				0 -> ItemStack(Items.AXOLOTL_BUCKET)
				1 -> custom("pearl_pristine")
				2 -> custom("anglr_treasure_mythic")
				3 -> custom("spirit_pure_${spiritFamily(spot)}")
				else -> perkStack(spot)
			}
		}
		if (pair == 40 || pair == 50) return perkStack(spot)
		if (AutoPin.matchingRule(spot) != null || hasSingle30(spot) || isChanceSpot(spot)) {
			return perkStack(spot)
		}
		return when (family) {
			0 -> ItemStack(FISH[Random(spot.id.toLong()).nextInt(FISH.size)])
			1 -> custom("pearl_rough")
			2 -> treasureLow(pair)
			3 -> custom("spirit_${spiritFamily(spot)}")
			else -> perkStack(spot)
		}
	}

	fun sameVisual(a: ItemStack, b: ItemStack): Boolean {
		if (a.item !== b.item) return false
		return a.get(DataComponents.ITEM_MODEL) == b.get(DataComponents.ITEM_MODEL)
	}

	private fun treasureLow(pair: Int): ItemStack = when (pair) {
		20 -> custom("anglr_treasure_uncommon")
		30 -> custom("anglr_treasure_rare")
		else -> custom("anglr_treasure_common")
	}

	private fun hasSingle30(spot: FishingSpot): Boolean {
		val types = spot.pairTypes() ?: return false
		return spot.perkValue(types.first) == 30 || spot.perkValue(types.second) == 30
	}

	private fun isChanceSpot(spot: FishingSpot): Boolean =
		spot.hasPerk(PerkType.ELUSIVE_CHANCE) || spot.grottoChance() != null

	private fun spiritFamily(spot: FishingSpot): String =
		SPIRIT_FAMILIES[Random(spot.id.toLong()).nextInt(SPIRIT_FAMILIES.size)]

	private fun perkStack(spot: FishingSpot): ItemStack {
		val perk = spot.primaryPerk() ?: return ItemStack(Items.PAPER)
		return custom("perk/${perk.type.name.lowercase()}")
	}

	private fun custom(path: String): ItemStack {
		val stack = ItemStack(Items.PAPER)
		stack.set(DataComponents.ITEM_MODEL, SpotFilter.id(path))
		return stack
	}
}
