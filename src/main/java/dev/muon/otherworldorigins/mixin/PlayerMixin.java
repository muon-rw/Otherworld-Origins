package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ShapeshiftPower;
import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Player.class)
public class PlayerMixin implements IEnchantmentSeedResettable {

    // also see ench_restrictions.PlayerMixin

    @Shadow(remap = true)
    protected int enchantmentSeed;

    @Override
    public void otherworld$resetEnchantmentSeed() {
        this.enchantmentSeed = ((Player) (Object) this).getRandom().nextInt();
    }

    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$preventArmorSet(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (!slot.isArmor() || stack.isEmpty()) return;
        if (!((Object) this instanceof ServerPlayer player)) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config != null && config.preventEquipment()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            ci.cancel();
        }
    }
}
