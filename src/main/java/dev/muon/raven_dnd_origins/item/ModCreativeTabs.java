package dev.muon.raven_dnd_origins.item;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RavenDndOrigins.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RAVEN_DND_ORIGINS = CREATIVE_MODE_TABS.register("raven_dnd_origins",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.raven_dnd_origins"))
                    .icon(() -> new ItemStack(ModItems.ORB_OF_VOCATION.get()))
                    .displayItems((params, output) ->
                            ModItems.ITEMS.getEntries().stream()
                                    // Icon-only items; keeping them out of every creative tab also hides them from JEI
                                    .filter(entry -> !entry.getId().getPath().startsWith("portrait/"))
                                    .forEach(entry -> output.accept(entry.get())))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
