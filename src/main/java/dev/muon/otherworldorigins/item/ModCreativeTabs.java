package dev.muon.otherworldorigins.item;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OtherworldOrigins.MODID);

    public static final RegistryObject<CreativeModeTab> OTHERWORLD_ORIGINS = CREATIVE_MODE_TABS.register("otherworld_origins",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.otherworldorigins"))
                    .icon(() -> new ItemStack(ModItems.ORB_OF_VOCATION.get()))
                    .displayItems((params, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
