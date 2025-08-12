package dev.muon.otherworldorigins.mixin.compat.thief;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.CharismaPower;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
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
    private static final Random RANDOM = new Random();

    @ModifyReturnValue(method = "commit", at = @At("RETURN"))
    private Crime.Outcome onDeclareThief(Crime.Outcome original, @Local(argsOnly = true) LivingEntity criminal) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(criminal);
        if (!original.punished()) return original;
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.CHARISMA.get());
            float totalCharisma = playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .map(CharismaPower.Configuration::amount)
                    .reduce(0f, Float::sum);

            if (totalCharisma > 0) {
                float chanceToAvoid = Math.min(totalCharisma, 1.0f);
                if (RANDOM.nextFloat() < chanceToAvoid) {
                    if (criminal instanceof Player player) {
                        player.displayClientMessage(Component.translatable("otherworld.message.charisma_save"), true);
                    }
                    return Crime.Outcome.NONE;
                }
            }
        }
        return original;
    }
}