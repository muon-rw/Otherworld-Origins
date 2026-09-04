package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.JumpCooldownAccess;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Limits how often the <strong>local client player</strong> can jump: after each jump they must wait
 * {@code cooldown} client ticks before the next jump. Enforced only on the client to avoid movement
 * rubber-banding; uses the largest {@code cooldown} among all active instances.
 */
public final class JumpCooldownPower extends PowerType<JumpCooldownPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("cooldown").forGetter(Configuration::cooldown)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (!(holder.rawOwner() instanceof Player player)) return;
        if (!PowerLookup.hasActive(player, ModPowers.JUMP_COOLDOWN)) {
            ((JumpCooldownAccess) player).raven_dnd_origins$setJumpCooldownRemaining(0);
        }
    }

    public static boolean shouldBlockJump(Player player) {
        if (!player.level().isClientSide() || !player.isLocalPlayer()) {
            return false;
        }
        if (!PowerLookup.hasActive(player, ModPowers.JUMP_COOLDOWN)) return false;
        return ((JumpCooldownAccess) player).raven_dnd_origins$getJumpCooldownRemaining() > 0;
    }

    public static void onSuccessfulJump(Player player) {
        if (!player.level().isClientSide() || !player.isLocalPlayer()) {
            return;
        }
        int[] cooldownTicks = new int[1];
        PowerLookup.forEach(player, ModPowers.JUMP_COOLDOWN, Configuration.class,
                cfg -> cooldownTicks[0] = Math.max(cooldownTicks[0], cfg.cooldown()));
        if (cooldownTicks[0] > 0) {
            ((JumpCooldownAccess) player).raven_dnd_origins$setJumpCooldownRemaining(cooldownTicks[0]);
        }
    }

    public record Configuration(int cooldown) {
    }
}
