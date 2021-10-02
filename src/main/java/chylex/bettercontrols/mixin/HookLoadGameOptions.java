package chylex.bettercontrols.mixin;
import chylex.bettercontrols.BetterControlsMod;
import chylex.bettercontrols.input.KeyBindingWithModifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class HookLoadGameOptions{
	private boolean hasLoaded = false;
	
	@Shadow
	public KeyMapping[] keyMappings;
	
	@Inject(method = "load()V", at = @At("HEAD"))
	private void load(final CallbackInfo info){
		if (hasLoaded){
			return;
		}
		
		hasLoaded = true;
		keyMappings = ArrayUtils.addAll(keyMappings, BetterControlsMod.config.getAllKeyBindings());
		AccessKeyBindingFields.getCategoryOrderMap().put(KeyBindingWithModifier.CATEGORY, Integer.valueOf(Integer.MAX_VALUE));
	}
}
