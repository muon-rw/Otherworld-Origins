package dev.muon.otherworldorigins.mixin.compat.justlevelingfork.bettercombat;

import net.bettercombat.client.collision.TargetFinder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

/**
 * Safe reimplementation of JustLevelingFork's MixTargetFinder, which we cancel
 * via {@link dev.muon.otherworldorigins.OtherworldOriginsMixinCanceller}.
 * The original crashes with NPE when the JLF entity-reach modifier is absent.
 */
@Mixin(value = TargetFinder.class, remap = false)
public class TargetFinderMixin {

    private static final UUID JLF_REACH_MODIFIER = UUID.fromString("96a891fe-5919-418d-8205-f50464391509");

    @ModifyVariable(
            method = "findAttackTargetResult",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static double otherworldorigins$addJlfReach(double attackRange, Player player) {
        AttributeInstance reachAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (reachAttr != null) {
            AttributeModifier mod = reachAttr.getModifier(JLF_REACH_MODIFIER);
            if (mod != null) {
                attackRange += mod.getAmount();
            }
        }
        return attackRange;
    }
}
