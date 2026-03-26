package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.power.ModifyMaxAirSupplyPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(Player.class)
public class PlayerMaxAirSupplyMixin {
    /** JLF's {@code MixPlayer} overrides {@code getMaxAirSupply} on Players via mixin, without calling super —
     * {@link dev.muon.otherworldorigins.mixin.EntityMixin} getMaxAirSupply never runs for players, so add it here
     * But only if JLF is present, since the method isn't actually overridden by Player otherwise. */
    @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget"})
    @ModifyReturnValue(method = "getMaxAirSupply", at = @At("RETURN"), require = 1)
    private int otherworldorigins$addApoliMaxAir(int original) {
        return original + ModifyMaxAirSupplyPower.getTotalAirBonus((Player) (Object) this);
    }
}
