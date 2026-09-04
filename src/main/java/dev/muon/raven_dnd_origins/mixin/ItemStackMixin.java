package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.power.GoldDurabilityPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import dev.muon.raven_dnd_origins.restrictions.SpellRestrictions;
import dev.muon.raven_dnd_origins.skills.ModSkills;
import dev.muon.raven_dnd_origins.util.ArtisanBrewNbt;
import dev.muon.raven_dnd_origins.util.MasterworkAffixNbt;
import dev.muon.raven_dnd_origins.util.SoulOfArtificeNbt;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.item.Scroll;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Unique
    private static final TagKey<Item> raven_dnd_origins$GOLDEN_GEAR =
            TagKey.create(Registries.ITEM, RavenDndOrigins.loc("golden_gear"));

    @Unique
    private static final TagKey<Block> raven_dnd_origins$ADVANCED_TABLES =
            TagKey.create(Registries.BLOCK, RavenDndOrigins.loc("advanced_tables"));

    @Unique
    private static final TagKey<Block> raven_dnd_origins$BASIC_TABLES =
            TagKey.create(Registries.BLOCK, RavenDndOrigins.loc("basic_tables"));

    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    private List<Component> raven_dnd_origins$annotateRestrictedTooltips(
            List<Component> original, Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag
    ) {
        if (player == null) {
            return original;
        }

        ItemStack stack = (ItemStack) (Object) this;
        List<Component> tooltips = new ArrayList<>(original);

        raven_dnd_origins$addEnchantmentTooltips(tooltips, stack, player);
        raven_dnd_origins$addSpellTooltips(tooltips, stack, player);
        raven_dnd_origins$addReforgeTableTooltips(tooltips, stack, player);
        raven_dnd_origins$addMarkerLine(tooltips, MasterworkAffixNbt.hasMasterwork(stack), "item.raven_dnd_origins.masterwork");
        raven_dnd_origins$addMarkerLine(tooltips, SoulOfArtificeNbt.isActive(stack), "item.raven_dnd_origins.artificers_soul");
        raven_dnd_origins$addMarkerLine(tooltips, ArtisanBrewNbt.has(stack), "item.raven_dnd_origins.artisan_brew");

        return tooltips;
    }

    @Unique
    private void raven_dnd_origins$addMarkerLine(List<Component> tooltips, boolean present, String translationKey) {
        if (!present) {
            return;
        }
        Component line = Component.translatable(translationKey).withStyle(ChatFormatting.LIGHT_PURPLE);
        tooltips.add(tooltips.isEmpty() ? 0 : 1, line);
    }

    @Unique
    private void raven_dnd_origins$addReforgeTableTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        Block block = Block.byItem(stack.getItem());
        boolean isAdvancedTable = block.defaultBlockState().is(raven_dnd_origins$ADVANCED_TABLES);
        boolean isBasicTable = block.defaultBlockState().is(raven_dnd_origins$BASIC_TABLES);
        if (!isAdvancedTable && !isBasicTable) {
            return;
        }

        boolean hasSkill = ModSkills.REFORGING.get().isEnabled(player);
        boolean isUnderdarkRace = raven_dnd_origins$isUnderdarkRace(player);

        if (isAdvancedTable && !(hasSkill && isUnderdarkRace)) {
            raven_dnd_origins$strikeThroughName(tooltips, "raven_dnd_origins.tooltip.advanced_table_restricted");
        } else if (isBasicTable && !(hasSkill || isUnderdarkRace)) {
            raven_dnd_origins$strikeThroughName(tooltips, "raven_dnd_origins.tooltip.basic_table_restricted");
        }
    }

    @Unique
    private void raven_dnd_origins$strikeThroughName(List<Component> tooltips, String reasonKey) {
        if (tooltips.isEmpty()) {
            return;
        }
        tooltips.set(0, Component.literal("").append(tooltips.get(0)).withStyle(ChatFormatting.STRIKETHROUGH));
        tooltips.add(1, Component.translatable(reasonKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Unique
    private void raven_dnd_origins$addEnchantmentTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return;
        }
        for (int i = 0; i < tooltips.size(); i++) {
            Component line = tooltips.get(i);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                String fullName = Enchantment.getFullname(enchantment, entry.getIntValue()).getString();
                if (!line.getString().contains(fullName)) {
                    continue;
                }
                if (EnchantmentRestrictions.isEnchantmentAllowed(player, enchantment)) {
                    continue;
                }
                tooltips.set(i, Component.literal("").append(line)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));

                String requiredClass = EnchantmentRestrictions.getRequiredClass(enchantment);
                if (requiredClass != null) {
                    String className = requiredClass.substring(0, 1).toUpperCase() + requiredClass.substring(1);
                    tooltips.add(i + 1, Component.literal("Only " + className + "s can use this enchantment")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
                break;
            }
        }
    }

    @Unique
    private void raven_dnd_origins$addSpellTooltips(List<Component> tooltips, ItemStack stack, Player player) {
        if (stack.getItem() instanceof Scroll) {
            AbstractSpell spell = ISpellContainer.get(stack).getSpellAtIndex(0).getSpell();
            if (spell != null && SpellRestrictions.isSpellRestrictedForDisplay(player, spell)) {
                MutableComponent warning = Component.literal("Can only be cast using a Scroll - ")
                        .append(SpellRestrictions.getRestrictionMessage(player, spell))
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
                tooltips.add(1, warning);
            }
            return;
        }
        if (!ISpellContainer.isSpellContainer(stack)) {
            return;
        }
        ISpellContainer container = ISpellContainer.get(stack);
        for (int i = 0; i < tooltips.size(); i++) {
            Component line = tooltips.get(i);
            for (SpellSlot slot : container.getActiveSpells()) {
                AbstractSpell spell = slot.getSpell();
                if (!line.getString().contains(spell.getDisplayName(null).getString())) {
                    continue;
                }
                if (!SpellRestrictions.isSpellRestrictedForDisplay(player, spell)) {
                    continue;
                }
                tooltips.set(i, Component.literal("").append(line)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
                tooltips.add(i + 1, Component.literal(" ")
                        .append(SpellRestrictions.getRestrictionMessage(player, spell))
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                break;
            }
        }
    }

    @Unique
    private boolean raven_dnd_origins$isUnderdarkRace(Player player) {
        PlayerOriginsImpl origins = PlayerOriginsAttachment.get(player);
        if (origins == null) {
            return false;
        }
        ResourceLocation subrace = origins.getOrigin(RavenDndOrigins.loc("subrace"));
        if (subrace == null) {
            return false;
        }
        String path = subrace.getPath();
        return path.contains("drow") || path.contains("deep") || path.contains("duergar");
    }

    @Unique
    private boolean raven_dnd_origins$isGoldenGear(ItemStack stack) {
        if (stack.is(raven_dnd_origins$GOLDEN_GEAR)) {
            return true;
        }
        if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial().is(ArmorMaterials.GOLD)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!path.contains("gold") && !path.contains("gilded")) {
            return false;
        }
        return stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                || stack.getItem() instanceof DiggerItem
                || stack.getItem() instanceof ArmorItem;
    }

    // The 1.20.1 power added its amount to the Unbreaking level fed into vanilla's single durability
    // roll. Fold the bonus into a boosted copy of the stack so one roll still runs at the combined
    // level; rolling again on vanilla's result would stack multiplicatively.
    @WrapOperation(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
            )
    )
    private int raven_dnd_origins$goldenGearDurability(
            ServerLevel level, ItemStack stack, int damage, Operation<Integer> original,
            @Local(argsOnly = true) @Nullable LivingEntity user
    ) {
        int bonusLevels = raven_dnd_origins$goldDurabilityBonus(stack, user);
        if (bonusLevels <= 0) {
            return original.call(level, stack, damage);
        }
        Holder<Enchantment> unbreaking = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING);
        ItemEnchantments.Mutable boostedEnchantments =
                new ItemEnchantments.Mutable(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        boostedEnchantments.set(unbreaking, boostedEnchantments.getLevel(unbreaking) + bonusLevels);
        ItemStack boosted = stack.copy();
        boosted.set(DataComponents.ENCHANTMENTS, boostedEnchantments.toImmutable());
        return original.call(level, boosted, damage);
    }

    @Unique
    private int raven_dnd_origins$goldDurabilityBonus(ItemStack stack, @Nullable LivingEntity user) {
        if (!(user instanceof Player player) || !raven_dnd_origins$isGoldenGear(stack)) {
            return 0;
        }
        int bonus = 0;
        for (GoldDurabilityPower.Configuration cfg
                : PowerLookup.active(player, ModPowers.GOLD_DURABILITY, GoldDurabilityPower.Configuration.class)) {
            bonus += cfg.amount();
        }
        return bonus;
    }
}
