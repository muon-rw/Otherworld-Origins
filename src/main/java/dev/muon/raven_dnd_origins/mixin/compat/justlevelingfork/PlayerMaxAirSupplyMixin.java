package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.power.ModifyMaxAirSupplyPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * JustLevelingFork's {@code MixPlayer} overrides {@code getMaxAirSupply} on Player without calling
 * super, so {@link dev.muon.raven_dnd_origins.mixin.EntityMixin}'s air bonus never reaches players
 * while JLF is installed. The higher priority makes this apply after MixPlayer, restoring the bonus
 * on top of JLF's athletics value. The suppression is required because MixPlayer adds the method
 * to Player at mixin-apply time, so the IDE cannot see it in the target class.
 */
@SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget"})
@Mixin(value = Player.class, priority = 1500)
public class PlayerMaxAirSupplyMixin {

    @ModifyReturnValue(method = "getMaxAirSupply", at = @At("RETURN"))
    private int raven_dnd_origins$addPowerAirBonus(int original) {
        return original + ModifyMaxAirSupplyPower.getTotalAirBonus((Player) (Object) this);
    }
}
