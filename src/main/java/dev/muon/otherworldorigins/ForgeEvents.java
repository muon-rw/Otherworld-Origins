package dev.muon.otherworldorigins;

import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.medieval.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworldorigins.entity.summons.SummonedSkeleton;
import dev.muon.otherworldorigins.entity.summons.SummonedWitherSkeleton;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyCriticalHitPower;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    private static final int CONE_DURATION_TICKS = 10;
    private static final Map<Integer, Integer> activeCones = new HashMap<>();


    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof SummonedSkeleton || entity instanceof SummonedWitherSkeleton) {
            ItemStack itemstack = entity.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemstack.isEmpty()) {
                event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), itemstack));
                entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

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

    public static void trackConeProjectile(AbstractConeProjectile coneProjectile) {
        activeCones.put(coneProjectile.getId(), 0);
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

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            checkAndOpenFeatScreen(serverPlayer, LevelingUtils.getPlayerLevel(serverPlayer));
        }
    }


    private static void checkAndOpenFeatScreen(ServerPlayer player, int playerLevel) {
        ResourceLocation layerId = null;
        if (playerLevel == 4) layerId = OtherworldOrigins.loc("first_feat");
        else if (playerLevel == 8) layerId = OtherworldOrigins.loc("second_feat");
        else if (playerLevel == 12) layerId = OtherworldOrigins.loc("third_feat");
        else if (playerLevel == 16) layerId = OtherworldOrigins.loc("fourth_feat");
        else if (playerLevel == 20) layerId = OtherworldOrigins.loc("fifth_feat");

        if (layerId != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

            if (layer != null) {
                IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
                if (originContainer != null) {
                    ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layer);
                    if (currentOrigin.location().equals(new ResourceLocation("origins", "empty"))) {
                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);

                        OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());

                        if (player.getServer() == null) return;

                        player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 1, () -> {
                            OriginsCommon.CHANNEL.send(target, originContainer.getSynchronizationPacket());
                            OriginsCommon.CHANNEL.send(target, new S2COpenOriginScreen(false));
                            originContainer.synchronize();
                        }));
                    }
                }
            }
        }
    }
}


