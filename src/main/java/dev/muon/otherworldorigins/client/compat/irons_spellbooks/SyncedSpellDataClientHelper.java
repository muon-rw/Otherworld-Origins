package dev.muon.otherworldorigins.client.compat.irons_spellbooks;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only resolution for {@link SyncedSpellData} owning players. Must not be referenced
 * from types or signatures in common mixins — only invoked behind a {@link Dist#CLIENT} guard
 * so dedicated servers never load this class.
 */
@OnlyIn(Dist.CLIENT)
public final class SyncedSpellDataClientHelper {
    private SyncedSpellDataClientHelper() {}

    public static Player resolveOwningPlayerFromClientWorld(SyncedSpellData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        int id = data.getServerPlayerId();
        return mc.level.getEntity(id) instanceof Player pl ? pl : null;
    }
}
