package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.muon.raven_dnd_origins.util.spell.RecentSpellCastCache;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.muon.raven_dnd_origins.power.ActionOnSpellCastPower;
import dev.muon.raven_dnd_origins.power.RecastSpellPower;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractSpell.class, remap = false)
public class AbstractSpellMixin {

    @WrapMethod(method = "onServerCastComplete")
    private void raven_dnd_origins$wrapOnServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled,
            Operation<Void> original
    ) {
        CastSource castSource = playerMagicData.getCastSource();
        CastType castType = playerMagicData.getCastType();
        original.call(level, spellLevel, entity, playerMagicData, cancelled);
        AbstractSpell self = (AbstractSpell) (Object) this;
        RecentSpellCastCache.recordCompletedCast(level, entity, self, spellLevel, cancelled);
        if (entity instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.onSpellEnd(serverPlayer);
        }
        ActionOnSpellCastPower.handleSpellCastComplete(self, entity, spellLevel, castSource, castType);
        RecastSpellPower.handleSpellCastComplete(self, entity, spellLevel, castSource, castType);
    }
}
