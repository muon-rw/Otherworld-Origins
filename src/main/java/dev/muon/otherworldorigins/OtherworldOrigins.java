package dev.muon.otherworldorigins;

import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.enchantment.ModEnchantments;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.sounds.ModSounds;
import dev.muon.otherworldorigins.spells.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

@Mod(OtherworldOrigins.MODID)
public class OtherworldOrigins
{
    public static final String MODID = "otherworldorigins";
    public static ResourceLocation loc(String id) {
        return new ResourceLocation(MODID, id);
    }
    public static final Logger LOGGER = LogUtils.getLogger();

    public OtherworldOrigins()
    {
        OtherworldOrigins.LOGGER.info("Loading Otherworld Origins");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModEnchantments.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        ModActions.register(modEventBus);
        ModConditions.register(modEventBus);
        ModPowers.register(modEventBus);

        ModSpells.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}
    }
}