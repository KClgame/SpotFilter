package kcl.spotfilter.client.audio

import kcl.spotfilter.client.config.SpotFilterConfig
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

object SpotSounds {
	fun playNewSpot() {
		if (!SpotFilterConfig.instance.enabled) return
		val client = Minecraft.getInstance()
		val player = client.player ?: return
		val level = client.level ?: return
		level.playLocalSound(
			player.x,
			player.y,
			player.z,
			SoundEvents.EXPERIENCE_ORB_PICKUP,
			SoundSource.PLAYERS,
			0.45f,
			1.15f,
			false
		)
	}
}
