package kcl.spotfilter.client.world

import com.mojang.math.Transformation
import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingSpot
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.scan.TextDisplays
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.slf4j.LoggerFactory

object PinnedSpotMarker {
	const val TAG = "spotfilter_marker"
	private val LOGGER = LoggerFactory.getLogger("spotfilter")
	private const val CLOSE_SCALE = 2.0f
	private const val FAR_SCALE = 7.0f
	private const val CLOSE_DIST = 8.0
	private const val FAR_DIST = 56.0
	private const val NAMETAG_BASE = 0.022f

	private var nextClientId = -910_001
	private val entities = HashMap<Int, Display.TextDisplay>()

	fun register() {
		LevelRenderEvents.COLLECT_SUBMITS.register { context ->
			if (!SpotFilterConfig.instance.enabled) return@register
			val client = Minecraft.getInstance()
			if (client.level == null || client.player == null) return@register
			val camera = context.levelState().cameraRenderState
			val collector = context.submitNodeCollector()
			val pose = context.poseStack()
			for (spot in SpotPool.pinned()) {
				if (spot.key.dimension != client.level!!.dimension().identifier()) continue
				val x = spot.x + 0.5
				val y = spot.y - 1.0
				val z = spot.z + 0.5
				val dx = x - camera.pos.x
				val dy = y - camera.pos.y
				val dz = z - camera.pos.z
				val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
				val scale = NAMETAG_BASE * (displayScale(dist) / CLOSE_SCALE)
				val label = Component.literal(distanceLabel(spot, dist))
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(spot.markerRgb())))
				pose.pushPose()
				try {
					pose.translate(dx, dy, dz)
					pose.scale(scale, scale, scale)
					collector.submitNameTag(
						pose,
						Vec3.ZERO,
						0,
						label,
						true,
						0xF000F0,
						camera
					)
				} finally {
					pose.popPose()
				}
			}
		}
	}

	fun isOurs(entity: Display.TextDisplay): Boolean =
		entity.entityTags().contains(TAG) || entities.values.any { it === entity }

	fun spawnOrUpdate(spot: FishingSpot) {
		if (!SpotFilterConfig.instance.enabled) return
		val client = Minecraft.getInstance()
		val level = client.level as? ClientLevel ?: return
		if (spot.key.dimension != level.dimension().identifier()) return

		val x = spot.x + 0.5
		val y = spot.y - 1.0
		val z = spot.z + 0.5
		val player = client.player
		val dist = if (player != null) {
			kotlin.math.sqrt(
				(x - player.x) * (x - player.x) +
					(y - player.y) * (y - player.y) +
					(z - player.z) * (z - player.z)
			)
		} else {
			CLOSE_DIST
		}
		val label = Component.literal(distanceLabel(spot, dist))
			.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(spot.markerRgb())))
		val scale = displayScale(dist)

		val current = entities[spot.id]
		if (current != null && !current.isRemoved && current.level() === level) {
			style(current, label, scale)
			current.snapTo(x, y, z)
			return
		}

		if (current != null) {
			discard(current)
			entities.remove(spot.id)
		}

		try {
			val entity = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level)
			entity.setId(nextSafeClientId())
			entity.addTag(TAG)
			entity.setBillboardConstraints(Display.BillboardConstraints.CENTER)
			entity.setViewRange(8.0f)
			entity.setBrightnessOverride(Brightness.FULL_BRIGHT)
			style(entity, label, scale)
			entity.snapTo(x, y, z)
			level.addEntity(entity)
			entities[spot.id] = entity
		} catch (e: Exception) {
			LOGGER.warn("Failed to spawn guide text_display for spot {}", spot.id, e)
		}
	}

	fun sync(spot: FishingSpot) {
		if (spot.pinned) {
			spawnOrUpdate(spot)
		} else {
			remove(spot.id)
		}
	}

	fun remove(id: Int) {
		entities.remove(id)?.let { discard(it) }
	}

	fun removeAll() {
		entities.values.forEach { discard(it) }
		entities.clear()
	}

	fun tick() {
		val client = Minecraft.getInstance()
		val level = client.level
		if (level == null || !SpotFilterConfig.instance.enabled) {
			removeAll()
			return
		}
		val keep = HashSet<Int>()
		for (spot in SpotPool.pinned()) {
			if (spot.key.dimension != level.dimension().identifier()) continue
			keep.add(spot.id)
			spawnOrUpdate(spot)
		}
		entities.keys.filter { it !in keep }.toList().forEach { remove(it) }
	}

	private fun nextSafeClientId(): Int {
		var id = nextClientId--
		if (id == 0) id = nextClientId--
		return id
	}

	private fun displayScale(dist: Double): Float {
		val t = ((dist - CLOSE_DIST) / (FAR_DIST - CLOSE_DIST)).coerceIn(0.0, 1.0)
		return (CLOSE_SCALE + (FAR_SCALE - CLOSE_SCALE) * t).toFloat()
	}

	private fun distanceLabel(spot: FishingSpot, dist: Double): String =
		"${spot.guideLabel()} ${dist.toInt()}m"

	private fun style(entity: Display.TextDisplay, label: Component, scale: Float) {
		entity.setText(label)
		TextDisplays.setSeeThrough(entity)
		entity.setBackgroundColor(0)
		entity.setTransformation(
			Transformation(null, null, Vector3f(scale, scale, scale), null)
		)
	}

	private fun discard(entity: Display.TextDisplay) {
		if (entity.isRemoved) return
		val level = entity.level()
		if (level is ClientLevel) {
			try {
				level.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
			} catch (e: Exception) {
				entity.discard()
			}
		} else {
			entity.discard()
		}
	}
}
