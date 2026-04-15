package dev.muon.otherworldorigins.mixin.compat.hardcorerevival;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public class AbstractSpellKnockoutMixin {

    @Inject(method = "attemptInitiateCast", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$blockCastWhileKnockedOut(
            ItemStack stack,
            int spellLevel,
            Level level,
            Player player,
            CastSource castSource,
            boolean triggerCooldown,
            String castingEquipmentSlot,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player != null && HardcoreRevival.getRevivalData(player).isKnockedOut()) {
            cir.setReturnValue(false);
        }
    }
}
