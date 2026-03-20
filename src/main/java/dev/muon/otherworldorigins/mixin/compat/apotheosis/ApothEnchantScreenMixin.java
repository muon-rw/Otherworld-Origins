package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Adds cost-reduction tooltip info and fixes the side panel to show the original
 * enchantment power level (before cost reduction) for quality-related displays.
 */
@Mixin(ApothEnchantScreen.class)
public abstract class ApothEnchantScreenMixin extends EnchantmentScreen {

    public ApothEnchantScreenMixin(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Final
    @Shadow(remap = false)
    protected ApothEnchantmentMenu menu;

    /**
     * Appends a "Cost reduced: X → Y" line to the enchantment slot hover tooltip.
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
                    ordinal = 0
            )
    )
    private void otherworldorigins$addCostReductionTooltip(
            GuiGraphics gfx, Font font, List<Component> list, int mouseX, int mouseY,
            Operation<Void> original) {
        float modifier = otherworldorigins$getCostModifier();
        if (modifier < 1f && modifier > 0f) {
            for (int slot = 0; slot < 3; slot++) {
                int displayedCost = this.menu.costs[slot];
                if (displayedCost > 0 && this.isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)) {
                    int originalCost = Math.round(displayedCost / modifier);
                    if (originalCost > displayedCost) {
                        list.add(Component.literal(""));
                        list.add(Component.translatable(
                                "tooltip.otherworldorigins.enchant_cost_reduced",
                                originalCost, displayedCost
                        ).withStyle(ChatFormatting.GREEN));
                    }
                    break;
                }
            }
        }
        original.call(gfx, font, list, mouseX, mouseY);
    }

    /**
     * Fixes the side-panel "Enchanting at Level X" and power range to reflect the
     * original enchantment power level rather than the reduced display cost.
     * The XP cost line (index 2) already correctly shows the reduced cost.
     * <p>
     * List layout at this point:
     * [0] "Enchanting at Level X"   — fix to original
     * [1] ""
     * [2] "XP Cost: ..."           — keep reduced (correct)
     * [3] "Power Range: min - max"  — fix to original
     * [4] "Item Ench Value: ..."
     * [5] "Clues: ..."
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantScreen;drawOnLeft(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;I)V",
                    ordinal = 3,
                    remap = false
            )
    )
    private void otherworldorigins$fixSidePanelLevel(
            ApothEnchantScreen instance, GuiGraphics gfx, List<Component> list, int y,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY) {
        float modifier = otherworldorigins$getCostModifier();
        if (modifier < 1f && modifier > 0f) {
            for (int slot = 0; slot < 3; slot++) {
                int displayedCost = this.menu.costs[slot];
                if (displayedCost > 0 && this.isHovering(60, 14 + 19 * slot, 108, 17, mouseX, mouseY)) {
                    int originalLevel = Math.round(displayedCost / modifier);
                    if (originalLevel > displayedCost) {
                        list.set(0, Component.literal(I18n.get("info.apotheosis.ench_at", originalLevel))
                                .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GREEN));

                        if (list.size() > 3) {
                            ApothEnchantmentMenu.TableStats stats =
                                    ((ApothEnchantmentMenuAccessor) this.menu).otherworldorigins$getStats();
                            float quanta = stats.quanta() / 100F;
                            float rectification = stats.rectification() / 100F;
                            float maxEterna = EnchantingStatRegistry.getAbsoluteMaxEterna();
                            int minPow = Math.round(Mth.clamp(
                                    originalLevel - originalLevel * (quanta - quanta * rectification),
                                    1, maxEterna * 4));
                            int maxPow = Math.round(Mth.clamp(
                                    originalLevel + originalLevel * quanta,
                                    1, maxEterna * 4));
                            list.set(3, Component.translatable("info.apotheosis.power_range",
                                    Component.literal("" + minPow).withStyle(ChatFormatting.DARK_RED),
                                    Component.literal("" + maxPow).withStyle(ChatFormatting.BLUE)));
                        }
                    }
                    break;
                }
            }
        }
        original.call(instance, gfx, list, y);
    }

    @Unique
    private float otherworldorigins$getCostModifier() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 1f;

        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer == null) return 1f;

        var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_ENCHANTMENT_COST.get());
        if (playerPowers.isEmpty()) return 1f;

        return playerPowers.stream()
                .map(holder -> holder.value().getConfiguration())
                .map(ModifyEnchantmentCostPower.Configuration::amount)
                .reduce(1f, (a, b) -> a * (1 - b));
    }
}
