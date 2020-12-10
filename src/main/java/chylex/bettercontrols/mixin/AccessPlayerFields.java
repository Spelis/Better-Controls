package chylex.bettercontrols.mixin;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface AccessPlayerFields{
	@Accessor("abilityResyncCountdown")
	void setTicksLeftToDoubleTapFlight(int value);
}
