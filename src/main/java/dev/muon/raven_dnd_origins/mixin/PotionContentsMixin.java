package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@code ArtisanBrewNbt} stores boosted copies of a potion's effects as custom effects on top of the
 * potion's own, which vanilla concatenates. Collapsing same-effect entries to the strongest instance
 * makes tooltips, colour and instantaneous effects match what actually gets applied.
 */
@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {

    @ModifyReturnValue(method = "getAllEffects", at = @At("RETURN"))
    private Iterable<MobEffectInstance> raven_dnd_origins$collapseDuplicateEffects(Iterable<MobEffectInstance> original) {
        List<MobEffectInstance> collapsed = raven_dnd_origins$collapse(original);
        return collapsed != null ? collapsed : original;
    }

    @Inject(method = "forEachEffect", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$forEachCollapsedEffect(Consumer<MobEffectInstance> action, CallbackInfo ci) {
        PotionContents self = (PotionContents) (Object) this;
        if (self.customEffects().isEmpty() || self.potion().isEmpty()) {
            return;
        }
        for (MobEffectInstance effect : self.getAllEffects()) {
            action.accept(new MobEffectInstance(effect));
        }
        ci.cancel();
    }

    @Unique
    @Nullable
    private static List<MobEffectInstance> raven_dnd_origins$collapse(Iterable<MobEffectInstance> effects) {
        Map<Holder<MobEffect>, MobEffectInstance> strongest = new LinkedHashMap<>();
        boolean duplicated = false;
        for (MobEffectInstance effect : effects) {
            MobEffectInstance existing = strongest.get(effect.getEffect());
            if (existing == null) {
                strongest.put(effect.getEffect(), effect);
                continue;
            }
            duplicated = true;
            if (raven_dnd_origins$outranks(effect, existing)) {
                strongest.put(effect.getEffect(), effect);
            }
        }
        return duplicated ? new ArrayList<>(strongest.values()) : null;
    }

    @Unique
    private static boolean raven_dnd_origins$outranks(MobEffectInstance candidate, MobEffectInstance current) {
        if (candidate.getAmplifier() != current.getAmplifier()) {
            return candidate.getAmplifier() > current.getAmplifier();
        }
        return candidate.getDuration() > current.getDuration();
    }
}
