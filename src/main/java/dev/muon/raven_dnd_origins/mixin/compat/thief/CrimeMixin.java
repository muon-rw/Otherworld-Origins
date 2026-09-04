package dev.muon.raven_dnd_origins.mixin.compat.thief;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.CharismaPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.overgrown.apoli.power.PowerLookup;
import io.github.mortuusars.thief.world.Crime;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

@Mixin(value = Crime.class, remap = false)
public class CrimeMixin {

    @Unique
    private static final Random raven_dnd_origins$RANDOM = new Random();

    @ModifyReturnValue(method = "commit", at = @At("RETURN"))
    private Crime.Outcome raven_dnd_origins$charismaSaveFromPunishment(Crime.Outcome original, @Local(argsOnly = true) LivingEntity criminal) {
        if (!original.punished()) return original;

        float totalCharisma = 0f;
        for (CharismaPower.Configuration cfg : PowerLookup.active(
                criminal, ModPowers.CHARISMA, CharismaPower.Configuration.class)) {
            totalCharisma += cfg.amount();
        }
        if (totalCharisma <= 0) return original;

        if (raven_dnd_origins$RANDOM.nextFloat() < Math.min(totalCharisma, 1.0f)) {
            if (criminal instanceof Player player) {
                player.displayClientMessage(Component.translatable("raven_dnd_origins.message.charisma_save"), true);
            }
            return Crime.Outcome.NONE;
        }
        return original;
    }
}
