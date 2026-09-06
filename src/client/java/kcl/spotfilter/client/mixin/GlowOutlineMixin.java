package kcl.spotfilter.client.mixin;

import kcl.spotfilter.client.world.GlowPigs;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class GlowOutlineMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void spotfilter$glowOutline(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
		int color = GlowPigs.outlineColor(entity);
		if (color != 0) {
			state.outlineColor = color;
		}
	}
}
