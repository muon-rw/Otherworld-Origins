package dev.muon.otherworldorigins.util;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Apotheosis adventure loot roll for the main-hand item, mirroring {@code /apoth lootify}:
 * clear affix map, then {@link LootController#createLootItem}.
 */
public final class LootReforgeLogic {

    private LootReforgeLogic() {
    }

    /**
     * Whether {@link #tryReforgeMainHand} can attempt a roll for this stack (Apotheosis adventure on,
     * non-empty, affix data loaded, valid {@link LootCategory}).
     */
    public static boolean isReforgeEligible(ItemStack stack) {
        if (!Apotheosis.enableAdventure) {
            return false;
        }
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

        var rarityHolder = AffixHelper.getRarity(stack);
        LootRarity target;
        if (rarityHolder.isBound()) {
            LootRarity current = rarityHolder.get();
            // Reroll at least at the item's tier; raise to character tier when that is higher ("map" tier uses the same level brackets).
            target = LootRarity.max(current, levelRarity);
        } else {
            target = levelRarity;
        }

        AffixHelper.setAffixes(stack, java.util.Collections.emptyMap());
        try {
            LootController.createLootItem(stack, target, player.level().getRandom());
        } catch (RuntimeException ex) {
            OtherworldOrigins.LOGGER.debug("reforge_held_item: no affixes for {} at {}", stack, target, ex);
            return false;
        }

        serverPlayer.getInventory().setChanged();
        serverPlayer.containerMenu.broadcastChanges();
        return true;
    }

    /**
     * Matches {@link dev.muon.otherworldorigins.condition.entity.PlayerLevelCondition} / subclass level brackets.
     */
    @Nullable
    public static LootRarity rarityForCharacterLevel(int level) {
        String legacyId;
        if (level <= 4) {
            legacyId = "common";
        } else if (level <= 8) {
            legacyId = "uncommon";
        } else if (level <= 12) {
            legacyId = "rare";
        } else if (level <= 16) {
            legacyId = "epic";
        } else if (level <= 19) {
            legacyId = "mythic";
        } else {
            legacyId = "ancient";
        }
        var holder = RarityRegistry.byLegacyId(legacyId);
        if (!holder.isBound()) {
            return RarityRegistry.getMinRarity().isBound() ? RarityRegistry.getMinRarity().get() : null;
        }
        return holder.get();
    }
}
