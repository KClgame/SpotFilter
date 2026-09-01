package kcl.spotfilter.client.scan

import kcl.spotfilter.client.config.SpotFilterConfig
import kcl.spotfilter.client.data.FishingWorld
import kcl.spotfilter.client.data.SpotKey
import kcl.spotfilter.client.data.SpotPool
import kcl.spotfilter.client.parse.SpotParser
import kcl.spotfilter.client.world.PinnedSpotMarker
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB

object SpotScanner {
	fun tick(client: Minecraft) {
		FishingWorld.tick(client)
		SpotPool.tickNormalClockReset()
		val level = client.level
		val player = client.player
		if (level == null || player == null || !SpotFilterConfig.instance.enabled) {
			PinnedSpotMarker.tick()
			return
		}

		val now = level.gameTime
		val range = client.options.getEffectiveRenderDistance() * 16.0
		val box = AABB(player.position(), player.position()).inflate(range.coerceAtLeast(48.0))
		val seen = HashSet<SpotKey>()

		for (entity in level.getEntities(net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY, box) { true }) {
			if (PinnedSpotMarker.isOurs(entity)) continue
			val parsed = SpotParser.parse(level, entity, TextDisplays.readText(entity), now) ?: continue
			val here = FishingWorld.current
			if (here != null && here.kind == parsed.kind) {
				parsed.place = here
			}
			seen.add(parsed.key)
			SpotPool.upsert(parsed)
		}

		SpotPool.dropMissingNearPlayer(seen, now)
		SpotPool.finishNormalScan(seen)
		PinnedSpotMarker.tick()
	}
}
