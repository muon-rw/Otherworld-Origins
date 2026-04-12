package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.muon.otherworldorigins.util.spell.RecentSpellCastCache;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import dev.muon.otherworldorigins.power.ActionOnSpellCastPower;
import dev.muon.otherworldorigins.power.RecastSpellPower;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSpell.class, remap = false)
public class AbstractSpellMixin {

    @Inject(method = "onServerCastTick", at = @At("HEAD"))
    private void onSpellTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        if (entity instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.onSpellTick(serverPlayer, playerMagicData);
        }
    }

    @WrapMethod(method = "onServerCastComplete")
    private void otherworldorigins$wrapOnServerCastComplete(
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
