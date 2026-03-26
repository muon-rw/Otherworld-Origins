package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.JumpCooldownAccess;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Limits how often the <strong>local client player</strong> can jump: after each jump they must wait
 * {@code cooldown} client ticks before the next jump. Enforced only on the client to avoid movement
 * rubber-banding; uses the largest {@code cooldown} among all active instances.
 */
public class JumpCooldownPower extends PowerFactory<JumpCooldownPower.Configuration> {
    public JumpCooldownPower() {
        super(Configuration.CODEC);
    }

    @Override
    protected void onLost(Configuration configuration, Entity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null || container.getPowers(ModPowers.JUMP_COOLDOWN.get()).isEmpty()) {
            ((JumpCooldownAccess) player).otherworldorigins$setJumpCooldownRemaining(0);
        }
    }

    public static boolean shouldBlockJump(Player player) {
        if (!player.level().isClientSide() || !player.isLocalPlayer()) {
            return false;
        }
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null || container.getPowers(ModPowers.JUMP_COOLDOWN.get()).isEmpty()) {
            return false;
        }
        return ((JumpCooldownAccess) player).otherworldorigins$getJumpCooldownRemaining() > 0;
    }

    public static void onSuccessfulJump(Player player) {
        if (!player.level().isClientSide() || !player.isLocalPlayer()) {
            return;
        }
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) {
            return;
        }
        int cooldownTicks = container.getPowers(ModPowers.JUMP_COOLDOWN.get()).stream()
                .mapToInt(holder -> holder.value().getConfiguration().cooldown())
                .max()
                .orElse(0);
        if (cooldownTicks > 0) {
            ((JumpCooldownAccess) player).otherworldorigins$setJumpCooldownRemaining(cooldownTicks);
        }
    }

    public record Configuration(int cooldown) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("cooldown").forGetter(Configuration::cooldown)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return cooldown > 0;
        }
    }
}
