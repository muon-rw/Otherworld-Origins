package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.capability.BrewerTrackerCapability;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyBrewedPotionPower;
import dev.muon.otherworldorigins.util.ArtisanBrewNbt;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

/**
 * Wraps the call to {@code BrewingRecipeRegistry.brewPotions} inside {@code doBrew}: diff bottle
 * slots before/after, look up the brewer from {@link BrewerTrackerCapability}, resolve their
 * {@link ModifyBrewedPotionPower} configurations, and stamp per-category bonuses onto only the
 * stacks whose Potion ID changed.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    @WrapOperation(
            // Ignore IDE complaint, do not set to remap = false
            method = "doBrew",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/brewing/BrewingRecipeRegistry;brewPotions(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;[I)V")
    )
    private static void otherworld$wrapBrewPotions(
            NonNullList<ItemStack> inputs, ItemStack ingredient, int[] inputIndexes, Operation<Void> original,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos
    ) {
        Potion[] pre = new Potion[3];
        for (int i = 0; i < 3 && i < inputs.size(); i++) {
            pre[i] = PotionUtils.getPotion(inputs.get(i));
        }
        original.call(inputs, ingredient, inputIndexes);
        otherworld$stampBonus(level, pos, inputs, pre);
    }

    @Unique
    private static void otherworld$stampBonus(Level level, BlockPos pos, NonNullList<ItemStack> items, Potion[] pre) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BrewingStandBlockEntity bsbe)) return;

        UUID brewerUuid = BrewerTrackerCapability.getBrewer(bsbe).orElse(null);
        if (brewerUuid == null) return;

        ServerPlayer brewer = serverLevel.getServer().getPlayerList().getPlayer(brewerUuid);
        if (brewer == null) return;

        IPowerContainer container = ApoliAPI.getPowerContainer(brewer);
        if (container == null) return;

        var holders = container.getPowers(ModPowers.MODIFY_BREWED_POTION.get());
        if (holders.isEmpty()) return;

        ArtisanBrewNbt.Bonus beneficial = ArtisanBrewNbt.Bonus.NONE;
        ArtisanBrewNbt.Bonus harmful = ArtisanBrewNbt.Bonus.NONE;
        ArtisanBrewNbt.Bonus neutral = ArtisanBrewNbt.Bonus.NONE;
        for (var holder : holders) {
            ModifyBrewedPotionPower.Configuration cfg = holder.value().getConfiguration();
            ArtisanBrewNbt.Bonus b = new ArtisanBrewNbt.Bonus(cfg.durationMultiplier(), cfg.amplifierModifier());
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.BENEFICIAL)) beneficial = beneficial.combine(b);
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.HARMFUL)) harmful = harmful.combine(b);
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.NEUTRAL)) neutral = neutral.combine(b);
        }
        if (beneficial.isNone() && harmful.isNone() && neutral.isNone()) return;

        for (int i = 0; i < 3 && i < items.size(); i++) {
            ItemStack stack = items.get(i);
            Potion post = PotionUtils.getPotion(stack);
            if (post == Potions.EMPTY) continue;
            if (post == pre[i]) continue;
            if (!otherworld$potionHasMatchingEffect(stack, beneficial, harmful, neutral)) continue;
            ArtisanBrewNbt.write(stack, brewerUuid, beneficial, harmful, neutral);
        }
    }

    @Unique
    private static boolean otherworld$potionHasMatchingEffect(
            ItemStack stack, ArtisanBrewNbt.Bonus beneficial, ArtisanBrewNbt.Bonus harmful, ArtisanBrewNbt.Bonus neutral
    ) {
        for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
            ArtisanBrewNbt.Bonus bonus = switch (effect.getEffect().getCategory()) {
                case BENEFICIAL -> beneficial;
                case HARMFUL -> harmful;
                case NEUTRAL -> neutral;
            };
            if (!bonus.isNone()) return true;
        }
        return false;
    }
}
