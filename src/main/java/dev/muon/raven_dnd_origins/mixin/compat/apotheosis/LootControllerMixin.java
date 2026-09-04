package dev.muon.raven_dnd_origins.mixin.compat.apotheosis;

import dev.muon.raven_dnd_origins.util.SoulOfArtificeNbt;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
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
            method = "createLootItem(Lnet/minecraft/world/item/ItemStack;Ldev/shadowsoffire/apotheosis/loot/LootCategory;Ldev/shadowsoffire/apotheosis/loot/LootRarity;Ldev/shadowsoffire/apotheosis/tiers/GenContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN")
    )
    private static void raven_dnd_origins$clearSoulOfArtificeAfterLootRoll(
            ItemStack stack,
            LootCategory cat,
            LootRarity rarity,
            GenContext ctx,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        SoulOfArtificeNbt.clearActive(stack);
    }
}
