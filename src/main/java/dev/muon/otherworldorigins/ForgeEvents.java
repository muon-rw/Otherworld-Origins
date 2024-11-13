package dev.muon.otherworldorigins;

import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyCriticalHitPower;
import dev.muon.otherworldorigins.util.SpellRestrictions;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    private static final int CONE_DURATION_TICKS = 10;
    private static final Map<Integer, Integer> activeCones = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            activeCones.entrySet().removeIf(entry -> {
                int coneId = entry.getKey();
                int age = entry.getValue();
                if (age >= CONE_DURATION_TICKS) {
                    for (Level level : event.getServer().getAllLevels()) {
                        AbstractConeProjectile cone = (AbstractConeProjectile) level.getEntity(coneId);
                        if (cone != null) {
                            cone.discard();
                            return true;
                        }
                    }
                    return true;
                } else {
                    entry.setValue(age + 1);
                    return false;
                }
            });
        }
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!SpellRestrictions.isSpellAllowed(event.getEntity(), SpellRegistry.getSpell(event.getSpellId()))) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    Component.literal("You are not attuned to this type of magic!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    public static void trackConeProjectile(AbstractConeProjectile coneProjectile) {
        activeCones.put(coneProjectile.getId(), 0);
    }

    // Reprompting manually with our own checks, server-side only -
    // Origins Forge seems to have some race conditions with synchronization
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
        if (layerRegistry == null) return;

        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
        if (originContainer == null) return;

        List<Holder<OriginLayer>> missingOriginLayers = new ArrayList<>();

        for (OriginLayer layer : layerRegistry) {
            Holder<OriginLayer> layerHolder = layerRegistry.getHolderOrThrow(
                    layerRegistry.getResourceKey(layer).orElseThrow()
            );

            ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layerHolder);
            if (currentOrigin == null || currentOrigin.location().equals(new ResourceLocation("origins", "empty"))) {
                Set<Holder<Origin>> availableOrigins = layer.origins(player);
                if (!availableOrigins.isEmpty()) {
                    missingOriginLayers.add(layerHolder);
                }
            }
        }

        if (!missingOriginLayers.isEmpty()) {
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> serverPlayer);
            OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
            OriginsCommon.CHANNEL.send(target, originContainer.getSynchronizationPacket());
            OriginsCommon.CHANNEL.send(target, new S2COpenOriginScreen(false));
            originContainer.synchronize();
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();

        if (event.getTarget() instanceof Player targetPlayer) {
            IPowerContainer targetPowerContainer = ApoliAPI.getPowerContainer(targetPlayer);
            if (targetPowerContainer != null && targetPowerContainer.hasPower(ModPowers.PREVENT_CRITICAL_HIT.get())) {
                event.setDamageModifier(1.0f);
                event.setResult(CriticalHitEvent.Result.DENY);
                return;
            }
        }

        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_CRITICAL_HIT.get());
            float totalModifier = playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .map(ModifyCriticalHitPower.Configuration::amount)
                    .reduce(0f, Float::sum);

            if (totalModifier != 0) {
                float newDamageModifier = event.getDamageModifier() * (1 + totalModifier);
                event.setDamageModifier(newDamageModifier);
                event.setResult(CriticalHitEvent.Result.ALLOW);
            }
        }

    }
}


