package kcl.spotfilter.client.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("getText")
	Component spotfilter$getText();

	@Invoker("setText")
	void spotfilter$setText(Component text);

	@Invoker("getFlags")
	byte spotfilter$getFlags();

	@Invoker("setFlags")
	void spotfilter$setFlags(byte flags);
}
