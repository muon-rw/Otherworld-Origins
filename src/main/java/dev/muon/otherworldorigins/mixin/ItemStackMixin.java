package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.power.GoldDurabilityPower;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    private List<Component> modifyEnchantmentTooltips(List<Component> original, Player player, TooltipFlag isAdvanced) {
        ItemStack stack = (ItemStack) (Object) this;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        List<Component> modifiedTooltips = new ArrayList<>(original);

        for (int i = 0; i < modifiedTooltips.size(); i++) {
            Component line = modifiedTooltips.get(i);
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if (line.getString().contains(enchantment.getFullname(entry.getValue()).getString())) {
                    if (!EnchantmentRestrictions.isEnchantmentAllowed(player, enchantment)) {
                        MutableComponent disabledLine = Component.literal("").append(line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
                        modifiedTooltips.set(i, disabledLine);

                        String requiredClass = EnchantmentRestrictions.getRequiredClass(enchantment);
                        if (requiredClass != null) {
                            String className = requiredClass.substring(0, 1).toUpperCase() + requiredClass.substring(1);
                            MutableComponent explanationLine = Component.literal("Only " + className + "s can use this enchantment")
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                            modifiedTooltips.add(i + 1, explanationLine);
                        }

                        break;
                    }
                }
            }
        }

        return modifiedTooltips;
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