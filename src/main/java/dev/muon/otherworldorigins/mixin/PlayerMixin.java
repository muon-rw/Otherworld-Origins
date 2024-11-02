package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(Player.class)
public class PlayerMixin implements IEnchantmentSeedResettable {

    @ModifyExpressionValue(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 1
    )
    private boolean otherworldorigins$restrictDamageEnchantments(boolean original, Entity pTarget) {
        Player self = (Player) (Object) this;;
        if (pTarget instanceof LivingEntity livingTarget) {
            ItemStack weapon = self.getMainHandItem();
            float damageReduction = 0;

            if (!EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.SHARPNESS)) {
                damageReduction += otherworld$calculateDamageBonus(weapon, Enchantments.SHARPNESS, MobType.UNDEFINED);
            }
            if (!EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.SMITE)) {
                damageReduction += otherworld$calculateDamageBonus(weapon, Enchantments.SMITE, MobType.UNDEAD);
            }
            if (!EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.BANE_OF_ARTHROPODS)) {
                damageReduction += otherworld$calculateDamageBonus(weapon, Enchantments.BANE_OF_ARTHROPODS, MobType.ARTHROPOD);
            }

            if (damageReduction > 0) {
                livingTarget.heal(damageReduction);
            }
        }
        return original;
    }

    @Unique
    private float otherworld$calculateDamageBonus(ItemStack weapon, Enchantment enchantment, MobType mobType) {
        int level = EnchantmentHelper.getTagEnchantmentLevel(enchantment, weapon);
        if (level > 0) {
            if (mobType == MobType.UNDEFINED) {
                return 1 + level * 0.5F;
            } else {
                return level * 1.5F;
            }
        }
        return 0;
    }


    @Shadow
    protected int enchantmentSeed;

    @Override
    public void otherworld$resetEnchantmentSeed() {
        this.enchantmentSeed = ((Player) (Object) this).getRandom().nextInt();
    }

}
