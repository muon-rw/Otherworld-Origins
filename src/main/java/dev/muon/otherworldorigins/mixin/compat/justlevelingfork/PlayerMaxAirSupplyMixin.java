package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * JLF's {@code MixPlayer} overrides {@code getMaxAirSupply} on Player without calling super,
 * breaking all other {@code @ModifyReturnValue} chains (e.g. Origins' air bonus in
 * {@link dev.muon.otherworldorigins.mixin.EntityMixin}).
 * <p>
 * We cancel {@code MixPlayer} via {@link dev.muon.otherworldorigins.OtherworldOriginsMixinCanceller}
 * and reimplement the athletics air bonus here as a composable modifier.
 */
@Mixin(Entity.class)
public class PlayerMaxAirSupplyMixin {

    @ModifyReturnValue(method = "getMaxAirSupply", at = @At("RETURN"))
    private int otherworldorigins$addJlfAthleticsAir(int original) {
        if (!((Object) this instanceof Player player)) {
            return original;
        }
        if (RegistrySkills.ATHLETICS == null) {
            return original;
        }
        try {
            Skill athletics = RegistrySkills.ATHLETICS.get();
            if (!athletics.isEnabled(player)) {
                return original;
            }
            int jlfAir = (int) (300.0 * athletics.getValue()[0]);
            return original + (jlfAir - 300);
        } catch (NullPointerException e) {
            // Entity.<init> calls getMaxAirSupply() before the player is fully constructed;
            // Skill.isEnabled accesses synched entity data that may not exist yet.
            return original;
        }
    }
}
