package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
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
}