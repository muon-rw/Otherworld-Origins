package dev.muon.otherworldorigins.capability;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class BrewerTrackerCapability {

    public static final Capability<IBrewerTracker> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = OtherworldOrigins.loc("brewer_tracker");

    private BrewerTrackerCapability() {
    }

    public static void register(IEventBus modEventBus, IEventBus forgeEventBus) {
        modEventBus.addListener(BrewerTrackerCapability::onRegister);
        forgeEventBus.addGenericListener(BlockEntity.class, BrewerTrackerCapability::onAttach);
    }

    private static void onRegister(net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent event) {
        event.register(IBrewerTracker.class);
    }

    private static void onAttach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BrewingStandBlockEntity) {
            event.addCapability(ID, new Provider());
        }
    }

    public static Optional<UUID> getBrewer(BlockEntity be) {
        return be.getCapability(CAPABILITY).map(IBrewerTracker::getBrewer).orElse(Optional.empty());
    }

    public static void setBrewer(BlockEntity be, UUID uuid) {
        be.getCapability(CAPABILITY).ifPresent(t -> {
            t.setBrewer(uuid);
            be.setChanged();
        });
    }

    private static final class Tracker implements IBrewerTracker {
        @Nullable private UUID brewer;

        @Override
        public Optional<UUID> getBrewer() {
            return Optional.ofNullable(brewer);
        }

        @Override
        public void setBrewer(@Nullable UUID brewerUuid) {
            this.brewer = brewerUuid;
        }
    }

    private static final class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private static final String KEY = "brewer";

        private final Tracker tracker = new Tracker();
        private final LazyOptional<IBrewerTracker> opt = LazyOptional.of(() -> tracker);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tracker.getBrewer().ifPresent(uuid -> tag.putUUID(KEY, uuid));
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            tracker.setBrewer(tag.hasUUID(KEY) ? tag.getUUID(KEY) : null);
        }
    }

}
