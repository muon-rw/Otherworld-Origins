package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.power.GoldDurabilityPower;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import dev.muon.otherworldorigins.skills.ModSkills;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Unique
    private AbstractSpell otherworld$getSpellFromScroll(ItemStack itemStack) {
        return ISpellContainer.get(itemStack).getSpellAtIndex(0).getSpell();
    }

    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    private List<Component> modifyTooltips(List<Component> original, Player player, TooltipFlag isAdvanced) {
        if (player == null) {
            return original;
        }

        ItemStack stack = (ItemStack) (Object) this;
        List<Component> modifiedTooltips = new ArrayList<>(original);

        otherworld$addEnchantmentTooltips(modifiedTooltips, stack, player);
        otherworld$addSpellTooltips(modifiedTooltips, stack, player);
        otherworld$addReforgeTableTooltips(modifiedTooltips, stack, player);

        return modifiedTooltips;
    }

    @Unique
    private void otherworld$addReforgeTableTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        Block block = Block.byItem(stack.getItem());
        boolean isAdvancedTable = block.defaultBlockState().is(TagKey.create(Registries.BLOCK, OtherworldOrigins.loc("advanced_tables")));
        boolean isBasicTable = block.defaultBlockState().is(TagKey.create(Registries.BLOCK, OtherworldOrigins.loc("basic_tables")));

        if (isAdvancedTable || isBasicTable) {
            boolean hasSkill = ModSkills.REFORGING.get().isEnabled(player);
            boolean isUnderdarkRace = otherworld$isUnderdarkRace(player);

            if (isAdvancedTable && !(hasSkill && isUnderdarkRace)) {
                tooltips.set(0, Component.literal("").append(tooltips.get(0))
                        .withStyle(ChatFormatting.STRIKETHROUGH));
                tooltips.add(1, Component.translatable("otherworldorigins.tooltip.advanced_table_restricted")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else if (isBasicTable && !(hasSkill || isUnderdarkRace)) {
                tooltips.set(0, Component.literal("").append(tooltips.get(0))
                        .withStyle(ChatFormatting.STRIKETHROUGH));
                tooltips.add(1, Component.translatable("otherworldorigins.tooltip.basic_table_restricted")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    @Unique
    private void otherworld$addEnchantmentTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        for (int i = 0; i < tooltips.size(); i++) {
            Component line = tooltips.get(i);
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if (line.getString().contains(enchantment.getFullname(entry.getValue()).getString())) {
                    if (!EnchantmentRestrictions.isEnchantmentAllowed(player, enchantment)) {
                        MutableComponent disabledLine = Component.literal("").append(line)
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
                        tooltips.set(i, disabledLine);

                        String requiredClass = EnchantmentRestrictions.getRequiredClass(enchantment);
                        if (requiredClass != null) {
                            String className = requiredClass.substring(0, 1).toUpperCase() + requiredClass.substring(1);
                            MutableComponent explanationLine = Component.literal("Only " + className + "s can use this enchantment")
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                            tooltips.add(i + 1, explanationLine);
                        }
                        break;
                    }
                }
            }
        }
    }

    @Unique
    private void otherworld$addSpellTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        if (stack.getItem() instanceof Scroll) {
            AbstractSpell spell = otherworld$getSpellFromScroll(stack);
            if (spell != null && !SpellRestrictions.isSpellAllowed(player, spell)) {
                MutableComponent warningText = Component.literal("Can only be cast using a Scroll - ")
                        .append(SpellRestrictions.getRestrictionMessage(player, spell))
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
                tooltips.add(1, warningText);
            }
        } else if (stack.getItem() instanceof SpellBook && ISpellContainer.isSpellContainer(stack)) {
            var spellContainer = ISpellContainer.get(stack);
            for (int i = 0; i < tooltips.size(); i++) {
                Component line = tooltips.get(i);
                for (var spellData : spellContainer.getActiveSpells()) {
                    AbstractSpell spell = spellData.getSpell();
                    String spellName = spell.getDisplayName(null).getString();
                    if (line.getString().contains(spellName)) {
                        if (!SpellRestrictions.isSpellAllowed(player, spell)) {
                            MutableComponent disabledLine = Component.literal("").append(line)
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
                            tooltips.set(i, disabledLine);

                            Component restrictionMessage = SpellRestrictions.getRestrictionMessage(player, spell);
                            MutableComponent restrictionText = Component.literal(" ")
                                    .append(restrictionMessage)
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                            tooltips.add(i + 1, restrictionText);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private boolean otherworld$isUnderdarkRace(Player player) {
        return IOriginContainer.get(player).resolve().map(container -> {
            ResourceLocation subraceLayerLoc = OtherworldOrigins.loc("subrace");
            ResourceKey<Origin> playerOrigin = container.getOrigin(ResourceKey.create(OriginsAPI.getLayersRegistry().key(), subraceLayerLoc));
            if (playerOrigin == null) return false;

            String path = playerOrigin.location().getPath();
            return path.contains("drow") || path.contains("deep") || path.contains("duergar");
        }).orElse(false);
    }

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I"))
    private int otherworldorigins$addUnbreaking(Enchantment enchantment, ItemStack stack, Operation<Integer> original, int pAmount, net.minecraft.util.RandomSource pRandom, ServerPlayer pUser) {
        int level = original.call(enchantment, stack);
        if (enchantment == Enchantments.UNBREAKING && pUser != null && otherworld$isGoldenGear(pUser.level(), stack)) {
            IPowerContainer powerContainer = ApoliAPI.getPowerContainer(pUser);
            if (powerContainer != null) {
                var playerPowers = powerContainer.getPowers(ModPowers.GOLD_DURABILITY.get());
                int totalUnbreaking = playerPowers.stream()
                        .map(holder -> holder.value().getConfiguration())
                        .mapToInt(GoldDurabilityPower.Configuration::amount)
                        .sum();
                level += totalUnbreaking;
            }
        }
        return level;
    }
    @Unique
    private static final TagKey<Item> GOLDEN_GEAR = TagKey.create(Registries.ITEM, OtherworldOrigins.loc("golden_gear"));

    @Unique
    private boolean otherworld$isGoldenGear(Level level, ItemStack stack) {
        return stack.is(GOLDEN_GEAR) ||
                ModConditions.IS_GOLDEN_WEAPON.get().check(NoConfiguration.INSTANCE, level, stack) ||
                ModConditions.IS_GOLDEN_TOOL.get().check(NoConfiguration.INSTANCE, level, stack) ||
                ModConditions.IS_GOLDEN_ARMOR.get().check(NoConfiguration.INSTANCE, level, stack);
    }
}