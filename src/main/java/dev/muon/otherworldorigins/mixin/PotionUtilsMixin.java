package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.util.ArtisanBrewNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@link ArtisanBrewNbt} off potion stacks and:
 *   1. Returns boosted {@link MobEffectInstance} copies from {@code getMobEffects(ItemStack)} so both
 *      consumption (via {@code PotionItem}, splash, lingering, tipped arrow) and tooltip rendering see
 *      the modified values without ever touching the underlying {@code Potion} ID or
 *      {@code CustomPotionEffects} NBT.
 *   2. Recolors the effect tooltip lines added by {@code addPotionTooltip} with light purple to
 *      signal the brew is enhanced. Bracketed by HEAD/TAIL injections so we restyle only the lines
 *      this call appended.
 */
@Mixin(PotionUtils.class)
public abstract class PotionUtilsMixin {

    @Unique
    private static final ThreadLocal<int[]> otherworld$tooltipBracket = new ThreadLocal<>();

    @ModifyReturnValue(method = "getMobEffects(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"))
    private static List<MobEffectInstance> otherworld$boostBrewedEffects(List<MobEffectInstance> original, ItemStack stack) {
        if (!ArtisanBrewNbt.has(stack) || original.isEmpty()) {
            return original;
        }
        List<MobEffectInstance> out = new ArrayList<>(original.size());
        for (MobEffectInstance effect : original) {
            ArtisanBrewNbt.Bonus bonus = ArtisanBrewNbt.getBonus(stack, effect.getEffect().getCategory());
            if (bonus.isNone()) {
                out.add(effect);
                continue;
            }
            int newDuration = effect.getEffect().isInstantenous()
                    ? effect.getDuration()
                    : Math.round(effect.getDuration() * bonus.durationMultiplier());
            int newAmplifier = Math.max(0, effect.getAmplifier() + bonus.amplifierAdd());
            out.add(new MobEffectInstance(
                    effect.getEffect(), newDuration, newAmplifier,
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()
            ));
        }
        return out;
    }

    @Inject(
            method = "addPotionTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;F)V",
            at = @At("HEAD")
    )
    private static void otherworld$captureTooltipStart(ItemStack stack, List<Component> tooltips, float durationFactor, CallbackInfo ci) {
        if (ArtisanBrewNbt.has(stack)) {
            otherworld$tooltipBracket.set(new int[]{tooltips.size()});
        }
    }

    @Inject(
            method = "addPotionTooltip(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;F)V",
            at = @At("TAIL")
    )
    private static void otherworld$recolorAddedLines(ItemStack stack, List<Component> tooltips, float durationFactor, CallbackInfo ci) {
        int[] bracket = otherworld$tooltipBracket.get();
        otherworld$tooltipBracket.remove();
        if (bracket == null) return;
        int start = bracket[0];
        for (int i = start; i < tooltips.size(); i++) {
            Component line = tooltips.get(i);
            MutableComponent restyled = Component.literal("").append(line).withStyle(ChatFormatting.LIGHT_PURPLE);
            tooltips.set(i, restyled);
        }
    }
}
