package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.util.spell.SyncedSpellDataOwner;
import dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks.accessor.SyncedSpellDataAccessor;
import dev.muon.raven_dnd_origins.power.EldritchKnowledgePower;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SyncedSpellData.class, remap = false)
public class SyncedSpellDataMixin {

    @ModifyReturnValue(method = "isSpellLearned", at = @At("RETURN"))
    private boolean raven_dnd_origins$eldritchKnowledge(boolean original, AbstractSpell spell) {
        if (original) {
            return original;
        }
        SchoolType school = spell.getSchoolType();
        if (school == null || !SchoolRegistry.ELDRITCH_RESOURCE.equals(school.getId())) {
            return original;
        }
        Player player = raven_dnd_origins$resolveOwningPlayer();
        if (player != null && EldritchKnowledgePower.has(player)) {
            return true;
        }
        return original;
    }

    @Unique
    private Player raven_dnd_origins$resolveOwningPlayer() {
        LivingEntity owner = ((SyncedSpellDataAccessor) (Object) this).raven_dnd_origins$getLivingEntity();
        if (owner instanceof Player p) {
            return p;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return SyncedSpellDataOwner.resolveFromClientWorld((SyncedSpellData) (Object) this);
        }
        return null;
    }
}
