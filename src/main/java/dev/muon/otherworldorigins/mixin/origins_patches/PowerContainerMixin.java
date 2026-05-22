package dev.muon.otherworldorigins.mixin.origins_patches;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.OriginStateDumper;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.common.component.PowerContainer;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = PowerContainer.class, remap = false)
public abstract class PowerContainerMixin {

    /** Below this many powers a shrink isn't worth alarming on — a mid-creation player is legitimately sparse. */
    @Unique
    private static final int OTHERWORLD$WIPE_ALARM_MIN_POWERS = 8;

    @WrapOperation(
            method = "addPower",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
            )
    )
    private void logSubpowersAsDebug(Logger instance, String s, Object o, Object p, Operation<Void> original) {
        instance.debug(s, o, p);
    }

    /**
     * Tripwire for the power-container desync. {@code handle} applies a server power sync, replacing
     * the container. Alarms once — with the call path and a full pre-wipe {@link OriginStateDumper}
     * snapshot — when that sync collapses a populated container to (near-)empty <em>while the
     * player's origins are all still intact</em>.
     *
     * <p>The origins-intact gate is the load-bearing filter. A reselection orb, an aptitude respec,
     * and the vanilla Orb of Origin all legitimately shrink the power container, but every one of
     * them does so by clearing origins first — so a shrink with origins still complete is the only
     * shape that is actually the desync, not an expected reselection.
     */
    @Inject(method = "handle", at = @At("HEAD"))
    private void otherworld$detectPowerWipe(Multimap<ResourceLocation, ResourceLocation> powerSources,
                                            Map<ResourceLocation, CompoundTag> data, CallbackInfo ci) {
        int before = ((IPowerContainer) (Object) this).getPowerTypes(true).size();
        int after = powerSources.keySet().size();
        if (before < OTHERWORLD$WIPE_ALARM_MIN_POWERS || after > before / 4) {
            return;
        }
        if (!(((IPowerContainer) (Object) this).getOwner() instanceof Player player)) {
            return;
        }
        if (!IOriginContainer.get(player).map(IOriginContainer::hasAllOrigins).orElse(false)) {
            return;
        }
        OtherworldOrigins.LOGGER.warn(
                "[PowerWipe] incoming power sync for {} collapses the container with origins intact: {} -> {} power(s); incoming={}",
                player.getName().getString(), before, after, powerSources.keySet(),
                new Throwable("power-container wipe tripwire — call path"));
        OriginStateDumper.dump(player, "CLIENT", null,
                "power-container wipe tripwire — pre-wipe snapshot (" + before + " -> " + after + ")");
    }
}
