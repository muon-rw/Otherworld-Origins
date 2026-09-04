package dev.muon.raven_dnd_origins.capability;

import com.mojang.serialization.Codec;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Tracks the most recent player to insert a brewing ingredient into a brewing stand, so a brew paused
 * by chunk unload or server restart still credits the right brewer when it completes.
 */
public final class BrewerTrackerCapability {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RavenDndOrigins.MODID);

    private static final Codec<Optional<UUID>> BREWER_CODEC = UUIDUtil.CODEC.optionalFieldOf("brewer").codec();

    public static final Supplier<AttachmentType<Optional<UUID>>> BREWER = ATTACHMENT_TYPES.register(
            "brewer_tracker",
            () -> AttachmentType.<Optional<UUID>>builder(Optional::empty)
                    .serialize(BREWER_CODEC, Optional::isPresent)
                    .build());

    private BrewerTrackerCapability() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Optional<UUID> getBrewer(BlockEntity be) {
        if (!(be instanceof BrewingStandBlockEntity)) {
            return Optional.empty();
        }
        return be.getExistingData(BREWER.get()).orElse(Optional.empty());
    }

    public static void setBrewer(BlockEntity be, UUID uuid) {
        if (!(be instanceof BrewingStandBlockEntity)) {
            return;
        }
        be.setData(BREWER.get(), Optional.ofNullable(uuid));
        be.setChanged();
    }
}
