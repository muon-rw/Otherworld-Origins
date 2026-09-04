package dev.muon.raven_dnd_origins.util.spell;

import dev.muon.raven_dnd_origins.client.compat.irons_spellbooks.SyncedSpellDataClientHelper;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps the client-only owner lookup out of {@code SyncedSpellDataMixin}: Mixin resolves every
 * class a mixin references while preparing it, so a dedicated server would fail on the client
 * helper before the dist check could run. Plain classes are only linked lazily.
 */
public final class SyncedSpellDataOwner {
    private SyncedSpellDataOwner() {}

    @Nullable
    public static Player resolveFromClientWorld(SyncedSpellData data) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }
        return SyncedSpellDataClientHelper.resolveOwningPlayerFromClientWorld(data);
    }
}
