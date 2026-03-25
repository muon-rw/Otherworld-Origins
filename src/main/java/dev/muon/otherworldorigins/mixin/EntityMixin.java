package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyMaxAirSupplyPower;
import dev.muon.otherworldorigins.power.PreventBlockSlowdownPower;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @ModifyReturnValue(method = "getMaxAirSupply", at = @At("RETURN"))
    private int otherworldorigins$modifyMaxAirSupply(int original) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return original;
        }
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) {
            return original;
        }
        int bonus = container.getPowers(ModPowers.MODIFY_MAX_AIR_SUPPLY.get()).stream()
                .map(holder -> holder.value().getConfiguration())
                .mapToInt(ModifyMaxAirSupplyPower.Configuration::bonus)
                .sum();
        return original + bonus;
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void otherworld$preventBlockSlowdown(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (PreventBlockSlowdownPower.shouldPreventSlowdown(self, state)) {
            ci.cancel();
        }
    }
}
