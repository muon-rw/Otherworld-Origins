package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_core.leveling.LevelingUtils;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Apotheosis loot roll for the main-hand item, mirroring {@code /apoth reforge}: clear the affix map,
 * then {@link LootController#createLootItem}.
 */
public final class LootReforgeLogic {

    private LootReforgeLogic() {
    }

    public static boolean isReforgeEligible(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (AffixRegistry.INSTANCE.getValues().isEmpty()) {
            return false;
        }
        return !LootCategory.forItem(stack).isNone();
    }

    public static boolean tryReforgeMainHand(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide()) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!isReforgeEligible(stack)) {
            return false;
        }

        int charLevel = Math.max(0, LevelingUtils.getPlayerLevel(player));
        LootRarity levelRarity = rarityForCharacterLevel(charLevel);
        if (levelRarity == null) {
            return false;
        }

        // Reroll at least at the item's own tier; raise to the character tier when that is higher.
        DynamicHolder<LootRarity> currentHolder = AffixHelper.getRarity(stack);
        LootRarity target = levelRarity;
        if (currentHolder.isBound() && currentHolder.get().sortIndex() > levelRarity.sortIndex()) {
            target = currentHolder.get();
        }

        AffixHelper.setAffixes(stack, ItemAffixes.EMPTY);
        try {
            LootController.createLootItem(stack, target, GenContext.forPlayer(player));
        } catch (RuntimeException ex) {
            RavenDndOrigins.LOGGER.debug("reforge_held_item: no affixes for {} at {}", stack, target, ex);
            return false;
        }

        serverPlayer.getInventory().setChanged();
        serverPlayer.containerMenu.broadcastChanges();
        return true;
    }

    /**
     * Matches {@link dev.muon.raven_dnd_origins.condition.entity.PlayerLevelCondition} level brackets.
     * Apotheosis 8.7 ships five rarities, so the old "ancient" bracket folds into mythic.
     */
    @Nullable
    public static LootRarity rarityForCharacterLevel(int level) {
        String rarityPath;
        if (level <= 4) {
            rarityPath = "common";
        } else if (level <= 8) {
            rarityPath = "uncommon";
        } else if (level <= 12) {
            rarityPath = "rare";
        } else if (level <= 16) {
            rarityPath = "epic";
        } else {
            rarityPath = "mythic";
        }
        DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(rarityId(rarityPath));
        if (holder.isBound()) {
            return holder.get();
        }
        var sorted = RarityRegistry.getSortedRarities();
        return sorted.isEmpty() ? null : sorted.getFirst();
    }

    private static ResourceLocation rarityId(String path) {
        return ResourceLocation.fromNamespaceAndPath(Apotheosis.MODID, path);
    }
}
