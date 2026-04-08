package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.muon.otherworldorigins.util.SoulOfArtificeNbt;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reforging, {@code /apoth lootify}, and other full loot rolls replace affix data; clear our marker so tooltips stay accurate.
 */
@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {

    @Inject(
            method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/adventure/loot/LootCategory;Ldev/shadowsoffire/apotheosis/adventure/loot/LootRarity;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private static void otherworldorigins$clearSoulOfArtificeAfterLootRoll(
            ItemStack stack,
            LootCategory cat,
            LootRarity rarity,
            RandomSource rand,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        SoulOfArtificeNbt.clearActive(stack);
    }
}
