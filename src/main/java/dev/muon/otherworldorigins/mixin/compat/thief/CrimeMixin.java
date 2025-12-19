package dev.muon.otherworldorigins.mixin.compat.thief;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.CharismaPower;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
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
        if (!original.punished()) return original;
        
        double totalCharisma = PowerHolderComponent.getPowers(criminal, CharismaPower.class).stream()
                .filter(CharismaPower::isActive)
                .mapToDouble(powerType -> ModifierUtil.applyModifiers(criminal, powerType.getModifiers(), 0.0))
                .sum();

        if (totalCharisma > 0) {
            float chanceToAvoid = Math.min((float) totalCharisma, 1.0f);
            if (RANDOM.nextFloat() < chanceToAvoid) {
                if (criminal instanceof Player player) {
                    player.displayClientMessage(Component.translatable("otherworld.message.charisma_save"), true);
                }
                return Crime.Outcome.NONE;
            }
        }
        return original;
    }
}
