package dev.muon.otherworldorigins.mixin.compat.monobank;

import dev.muon.otherworldorigins.power.CharismaPower;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.mortuusars.monobank.Thief;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(value = Thief.class, remap = false)
public class ThiefMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "declareThief", at = @At("HEAD"), cancellable = true)
    private static void onDeclareThief(LivingEntity offender, List<LivingEntity> witnesses, Thief.Offence offence, CallbackInfo ci) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(offender);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.CHARISMA.get());
            float totalCharisma = playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .map(CharismaPower.Configuration::amount)
                    .reduce(0f, Float::sum);

            if (totalCharisma > 0) {
                float chanceToAvoid = Math.min(totalCharisma, 1.0f);
                if (RANDOM.nextFloat() < chanceToAvoid) {
                    if (offender instanceof Player player) {
                        player.displayClientMessage(Component.translatable("otherworld.message.charisma_save"), true);
                    }
                    ci.cancel();
                }
            }
        }
    }
}