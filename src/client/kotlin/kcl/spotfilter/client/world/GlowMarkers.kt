package kcl.spotfilter.client.world

import com.mojang.math.Transformation
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.highlight.HighlightMode
import kcl.spotfilter.client.highlight.HighlightState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f

object GlowMarkers {
	const val TAG = "spotfilter_glow_marker"
	private var nextClientId = -930_001
	private val entities = HashMap<Int, Display.ItemDisplay>()
	private val colors = HashMap<Int, Int>()
	private val SCALE = Transformation(null, null, Vector3f(3f, 3f, 3f), null)
	private val BRIGHT = Brightness(15, 15)

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
			!kcl.spotfilter.client.data.FishingWorld.overlayOn() ||
			!SpotFilterConfig.instance.highlightEnabled ||
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
		val x = spot.x + 0.5
		val y = spot.y + 5.0
		val z = spot.z + 0.5
		val rgb = spot.markerRgb()
		colors[spot.id] = rgb
		val stack = MarkerIcon.stackFor(spot)
		val current = entities[spot.id]
		if (current != null && !current.isRemoved && current.level() === level) {
			style(current, rgb, stack)
			current.snapTo(x, y, z)
			current.setDeltaMovement(0.0, 0.0, 0.0)
			return
		}
		if (current != null) {
			discard(current)
			entities.remove(spot.id)
		}
		try {
			val display = Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level)
			display.setId(nextSafeClientId())
			display.addTag(TAG)
			style(display, rgb, stack)
			display.snapTo(x, y, z)
			level.addEntity(display)
			entities[spot.id] = display
		} catch (_: Exception) {
		}
	}

	private fun style(display: Display.ItemDisplay, rgb: Int, stack: ItemStack) {
		display.setGlowingTag(true)
		display.setSilent(true)
		display.setInvulnerable(true)
		display.setNoGravity(true)
		display.noPhysics = true
		display.setItemTransform(ItemDisplayContext.FIXED)
		display.setBillboardConstraints(Display.BillboardConstraints.VERTICAL)
		display.setTransformation(SCALE)
		display.setWidth(4f)
		display.setHeight(4f)
		display.setViewRange(3f)
		display.setBrightnessOverride(BRIGHT)
		display.setGlowColorOverride(rgb)
		if (!MarkerIcon.sameVisual(display.itemStack, stack)) {
			display.setItemStack(stack.copy())
		}
	}

	private fun nextSafeClientId(): Int {
		var id = nextClientId--
		if (id == 0) id = nextClientId--
		return id
	}

	private fun discard(entity: Display.ItemDisplay) {
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
