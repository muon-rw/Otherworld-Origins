package dev.muon.otherworldorigins;

import dev.muon.otherworldorigins.entity.summons.SummonedSkeleton;
import dev.muon.otherworldorigins.entity.summons.SummonedWitherSkeleton;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
}

