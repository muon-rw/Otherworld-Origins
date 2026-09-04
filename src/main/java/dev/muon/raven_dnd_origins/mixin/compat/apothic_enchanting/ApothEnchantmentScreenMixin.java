package dev.muon.raven_dnd_origins.mixin.compat.apothic_enchanting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.IncreaseEnchantingLevelsPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.power.ModifyEnchantmentCostPower;
import dev.overgrown.apoli.power.PowerLookup;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentScreen;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Adds cost-reduction tooltip info, {@link IncreaseEnchantingLevelsPower} clue level breakdown,
 * and fixes the side panel to show the original enchantment power level (before cost reduction)
 * for quality-related displays.
 */
@Mixin(value = ApothEnchantmentScreen.class, remap = false)
public abstract class ApothEnchantmentScreenMixin extends EnchantmentScreen {

    @Unique
    private static final float raven_dnd_origins$MAX_POWER = 200f;

    public ApothEnchantmentScreenMixin(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Final
    @Shadow
    protected ApothEnchantmentMenu menu;

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
                    ordinal = 0,
                    remap = true
            )
    )
    private void raven_dnd_origins$addCostReductionTooltip(
            GuiGraphics gfx, Font font, List<Component> list, int mouseX, int mouseY,
            Operation<Void> original) {
        float modifier = raven_dnd_origins$getCostModifier();
        if (modifier < 1f && modifier > 0f) {
            int slot = raven_dnd_origins$hoveredSlot(mouseX, mouseY);
            if (slot >= 0) {
                int displayedCost = this.menu.costs[slot];
                int originalCost = Math.round(displayedCost / modifier);
                if (originalCost > displayedCost) {
                    list.add(Component.literal(""));
                    list.add(Component.translatable(
                            "tooltip.raven_dnd_origins.enchant_cost_reduced",
                            originalCost, displayedCost
                    ).withStyle(ChatFormatting.GREEN));
                }
            }
        }
        original.call(gfx, font, list, mouseX, mouseY);
    }

    /**
     * Side panel list layout at this point:
     * [0] "Enchanting at Level X"   fixed back to the pre-reduction level
     * [1] ""
     * [2] "XP Cost: ..."            kept reduced, which is correct
     * [3] "Power Range: min - max"  fixed back to the pre-reduction level
     * [4] "Item Ench Value: ..."
     * [5] "Clues: ..."
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentScreen;drawOnLeft(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;I)V",
                    ordinal = 3
            )
    )
    private void raven_dnd_origins$fixSidePanelLevel(
            ApothEnchantmentScreen instance, GuiGraphics gfx, List<Component> list, int y,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY) {
        float modifier = raven_dnd_origins$getCostModifier();
        if (modifier < 1f && modifier > 0f) {
            int slot = raven_dnd_origins$hoveredSlot(mouseX, mouseY);
            if (slot >= 0) {
                int displayedCost = this.menu.costs[slot];
                int originalLevel = Math.round(displayedCost / modifier);
                if (originalLevel > displayedCost) {
                    list.set(0, Component.translatable("info.apothic_enchanting.ench_at", originalLevel)
                            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GREEN));

                    if (list.size() > 3) {
                        EnchantmentTableStats stats =
                                ((ApothEnchantmentMenuAccessor) this.menu).raven_dnd_origins$getStats();
                        float quanta = stats.quanta() / 100F;
                        int minPow = stats.stable()
                                ? originalLevel
                                : Math.round(Mth.clamp(originalLevel - originalLevel * quanta, 1, raven_dnd_origins$MAX_POWER));
                        int maxPow = Math.round(Mth.clamp(originalLevel + originalLevel * quanta, 1, raven_dnd_origins$MAX_POWER));
                        list.set(3, Component.translatable("info.apothic_enchanting.power_range",
                                Component.literal("" + minPow).withStyle(ChatFormatting.DARK_RED),
                                Component.literal("" + maxPow).withStyle(ChatFormatting.BLUE)));
                    }
                }
            }
        }
        original.call(instance, gfx, list, y);
    }

    @Unique
    private int raven_dnd_origins$hoveredSlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < 3; slot++) {
            if (this.menu.costs[slot] > 0 && this.isHovering(60, 14 + 19 * slot, 108, 18, mouseX, mouseY)) {
                return slot;
            }
        }
        return -1;
    }

    @Unique
    private float raven_dnd_origins$getCostModifier() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 1f;
        float[] modifier = {1f};
        PowerLookup.forEach(player, ModPowers.MODIFY_ENCHANTMENT_COST, ModifyEnchantmentCostPower.Configuration.class,
                cfg -> modifier[0] *= (1 - cfg.amount()));
        return modifier[0];
    }

    /**
     * The clue list uses {@link Enchantment#getFullname(Holder, int)}; replace it with a line that
     * carries the base and power-granted parts when {@link IncreaseEnchantingLevelsPower} is active.
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getFullname(Lnet/minecraft/core/Holder;I)Lnet/minecraft/network/chat/Component;",
                    ordinal = 0,
                    remap = true
            )
    )
    private Component raven_dnd_origins$clueEnchantmentName(
            Holder<Enchantment> enchantment, int level, Operation<Component> original) {
        int bonus = IncreaseEnchantingLevelsPower.getBonus(this.minecraft.player);
        if (bonus <= 0) {
            return original.call(enchantment, level);
        }
        return IncreaseEnchantingLevelsPower.formatClueLine(enchantment, level, bonus);
    }
}
