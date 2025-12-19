package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.ModifyDurabilityChangePower;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import dev.muon.otherworldorigins.skills.ModSkills;
import io.github.apace100.apoli.access.EntityLinkedItemStack;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = ItemStack.class, priority = 1500)
public class ItemStackMixin {

    @Unique
    @SuppressWarnings("all")
    private static Entity otherworld$getEntityFromItemStack(ItemStack stack) {
        EntityLinkedItemStack heldStack = (EntityLinkedItemStack) (Object) stack;
        return heldStack.getEntity();
    }

    @Unique
    private ItemStack otherworld$getSelf() {
        return (ItemStack) (Object) this;
    }

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
        OriginComponent originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.maybeGet(player).orElse(null);
        if (originComponent == null) {
            return false;
        }

        ResourceLocation subraceLayerLoc = OtherworldOrigins.loc("subrace");
        OriginLayer layer = null; // OriginLayers.getLayer(subraceLayerLoc);
        Origin playerOrigin = originComponent.getOrigin(layer);
        
        if (playerOrigin == null) {
            return false;
        }

        String path = playerOrigin.getIdentifier().getPath();
        return path.contains("drow") || path.contains("deep") || path.contains("duergar");
    }

    @ModifyVariable(method = "setDamageValue", at = @At(value = "HEAD"), argsOnly = true)
    private int otherworld$modifyDurabilityChange(int damage) {
        ItemStack self = otherworld$getSelf();
        if (self == null) return damage;
        Entity entity = otherworld$getEntityFromItemStack(self);
        if (entity instanceof LivingEntity living) {
            CompoundTag tag = self.getOrCreateTag();
            int previousDamage = tag.contains("Damage", Tag.TAG_INT) ? tag.getInt("Damage") : 0;
            final int originalDurabilityChange = damage - previousDamage;

            // Apply modifiers from powers that match the conditions
            float modifiedValue = PowerHolderComponent.modify(living, ModifyDurabilityChangePower.class, (float) originalDurabilityChange,
                    p -> p.doesApply(living.level(), self, originalDurabilityChange));

            // Apply post-function from the first matching power (if any)
            int finalDurabilityChange = originalDurabilityChange;
            for (ModifyDurabilityChangePower power : PowerHolderComponent.getPowers(living, ModifyDurabilityChangePower.class)) {
                if (power.doesApply(living.level(), self, originalDurabilityChange)) {
                    finalDurabilityChange = power.postFunction(modifiedValue);
                    break;
                }
            }

            return previousDamage + finalDurabilityChange;
        }
        return damage;
    }
}
