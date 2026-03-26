package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.JumpCooldownPower;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import dev.muon.otherworldorigins.util.JumpCooldownAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Player.class)
public class PlayerMixin implements IEnchantmentSeedResettable, JumpCooldownAccess {

    // also see ench_restrictions.PlayerMixin

    @Unique
    private int otherworldorigins$jumpCooldownRemaining;

    @Shadow(remap = true)
    protected int enchantmentSeed;

    @Override
    public int otherworldorigins$getJumpCooldownRemaining() {
        return this.otherworldorigins$jumpCooldownRemaining;
    }

    @Override
    public void otherworldorigins$setJumpCooldownRemaining(int ticks) {
        this.otherworldorigins$jumpCooldownRemaining = Math.max(0, ticks);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$blockJumpDuringCooldown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (JumpCooldownPower.shouldBlockJump(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void otherworldorigins$startJumpCooldown(CallbackInfo ci) {
        JumpCooldownPower.onSuccessfulJump((Player) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void otherworldorigins$tickJumpCooldown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() || !self.isLocalPlayer()) {
            return;
        }
        if (this.otherworldorigins$jumpCooldownRemaining > 0) {
            this.otherworldorigins$jumpCooldownRemaining--;
        }
    }

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
