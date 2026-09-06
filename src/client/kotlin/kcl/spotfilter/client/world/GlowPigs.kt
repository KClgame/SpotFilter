package kcl.spotfilter.client.world

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.highlight.HighlightMode
import kcl.spotfilter.client.highlight.HighlightState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.animal.pig.Pig

object GlowPigs {
	const val TAG = "spotfilter_glow_pig"
	private var nextClientId = -930_001
	private val entities = HashMap<Int, Pig>()
	private val colors = HashMap<Int, Int>()

	@JvmStatic
	fun isOurs(entity: Entity): Boolean =
		entity.entityTags().contains(TAG) || entities.values.any { it === entity }

	@JvmStatic
	fun outlineColor(entity: Entity): Int {
		if (!isOurs(entity)) return 0
		val id = entities.entries.firstOrNull { it.value === entity }?.key ?: return 0
		val rgb = colors[id] ?: return 0
		return 0xFF000000.toInt() or rgb
	}

	fun tick() {
		val client = Minecraft.getInstance()
		val level = client.level
		if (level == null ||
			!SpotFilterConfig.instance.enabled ||
			SpotFilterConfig.instance.highlightMode() != HighlightMode.GLOWING
		) {
			removeAll()
			return
		}
		HighlightState.prune()
		val keep = HashSet<Int>()
		for (spot in HighlightState.litSpots()) {
			if (spot.key.dimension != level.dimension().identifier()) continue
			keep.add(spot.id)
			spawnOrUpdate(spot)
		}
		entities.keys.filter { it !in keep }.toList().forEach { remove(it) }
	}

	fun remove(id: Int) {
		entities.remove(id)?.let { discard(it) }
		colors.remove(id)
	}

	fun removeAll() {
		entities.values.forEach { discard(it) }
		entities.clear()
		colors.clear()
	}

	private fun spawnOrUpdate(spot: FishingSpot) {
		val client = Minecraft.getInstance()
		val level = client.level ?: return
		val x = PinnedSpotMarker.worldX(spot)
		val y = PinnedSpotMarker.worldY(spot)
		val z = PinnedSpotMarker.worldZ(spot)
		colors[spot.id] = spot.markerRgb()
		val current = entities[spot.id]
		if (current != null && !current.isRemoved && current.level() === level) {
			style(current)
			current.snapTo(x, y, z)
			current.setDeltaMovement(0.0, 0.0, 0.0)
			return
		}
		if (current != null) {
			discard(current)
			entities.remove(spot.id)
		}
		try {
			val pig = Pig(EntityTypes.PIG, level)
			pig.setId(nextSafeClientId())
			pig.addTag(TAG)
			style(pig)
			pig.snapTo(x, y, z)
			level.addEntity(pig)
			entities[spot.id] = pig
		} catch (_: Exception) {
		}
	}

	private fun style(pig: Pig) {
		pig.setInvisible(true)
		pig.setGlowingTag(true)
		pig.setNoAi(true)
		pig.setNoGravity(true)
		pig.setSilent(true)
		pig.setInvulnerable(true)
		pig.noPhysics = true
		pig.setPersistenceRequired()
	}

	private fun nextSafeClientId(): Int {
		var id = nextClientId--
		if (id == 0) id = nextClientId--
		return id
	}

	private fun discard(entity: Pig) {
		if (entity.isRemoved) return
		val level = entity.level()
		if (level is ClientLevel) {
			try {
				level.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
			} catch (_: Exception) {
				entity.discard()
			}
		} else {
			entity.discard()
		}
	}
}
