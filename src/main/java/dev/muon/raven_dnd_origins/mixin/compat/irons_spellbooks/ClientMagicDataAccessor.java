package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientMagicData.class, remap = false)
public interface ClientMagicDataAccessor {
    @Accessor("playerMagicData")
    static MagicData raven_dnd_origins$getPlayerMagicData() {
        throw new AssertionError();
    }
}
