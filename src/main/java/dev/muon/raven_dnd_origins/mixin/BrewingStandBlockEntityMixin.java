package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.capability.BrewerTrackerCapability;
import dev.muon.raven_dnd_origins.power.ModifyBrewedPotionPower;
import dev.muon.raven_dnd_origins.util.ArtisanBrewNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

/**
 * Wraps each bottle-slot mix inside {@code doBrew}: when the mix actually produced a new potion, look up
 * the brewer recorded on the stand and stamp their {@link ModifyBrewedPotionPower} bonuses onto the result.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    @WrapOperation(
            method = "doBrew",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack raven_dnd_origins$stampArtisanBrew(
            PotionBrewing brewing, ItemStack ingredient, ItemStack input, Operation<ItemStack> original,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos
    ) {
        ItemStack result = original.call(brewing, ingredient, input);
        if (result == input || result.isEmpty()) {
            return result;
        }
        raven_dnd_origins$stampBonus(level, pos, result);
        return result;
    }

    @Unique
    private static void raven_dnd_origins$stampBonus(Level level, BlockPos pos, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BrewingStandBlockEntity)) return;

        UUID brewerUuid = BrewerTrackerCapability.getBrewer(be).orElse(null);
        if (brewerUuid == null) return;

        ServerPlayer brewer = serverLevel.getServer().getPlayerList().getPlayer(brewerUuid);
        if (brewer == null) return;

        List<ModifyBrewedPotionPower.Configuration> configs = ModifyBrewedPotionPower.getActiveConfigs(brewer);
        if (configs.isEmpty()) return;

        ArtisanBrewNbt.Bonus beneficial = ArtisanBrewNbt.Bonus.NONE;
        ArtisanBrewNbt.Bonus harmful = ArtisanBrewNbt.Bonus.NONE;
        ArtisanBrewNbt.Bonus neutral = ArtisanBrewNbt.Bonus.NONE;
        for (ModifyBrewedPotionPower.Configuration cfg : configs) {
            ArtisanBrewNbt.Bonus b = new ArtisanBrewNbt.Bonus(cfg.durationMultiplier(), cfg.amplifierModifier());
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.BENEFICIAL)) beneficial = beneficial.combine(b);
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.HARMFUL)) harmful = harmful.combine(b);
            if (ModifyBrewedPotionPower.appliesTo(cfg, MobEffectCategory.NEUTRAL)) neutral = neutral.combine(b);
        }
        if (beneficial.isNone() && harmful.isNone() && neutral.isNone()) return;
        if (!raven_dnd_origins$hasMatchingEffect(stack, beneficial, harmful, neutral)) return;

        ArtisanBrewNbt.write(stack, brewerUuid, beneficial, harmful, neutral);
    }

    @Unique
    private static boolean raven_dnd_origins$hasMatchingEffect(
            ItemStack stack, ArtisanBrewNbt.Bonus beneficial, ArtisanBrewNbt.Bonus harmful, ArtisanBrewNbt.Bonus neutral
    ) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            ArtisanBrewNbt.Bonus bonus = switch (effect.getEffect().value().getCategory()) {
                case BENEFICIAL -> beneficial;
                case HARMFUL -> harmful;
                case NEUTRAL -> neutral;
            };
            if (!bonus.isNone()) return true;
        }
        return false;
    }
}
