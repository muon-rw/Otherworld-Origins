package dev.muon.otherworldorigins;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class OtherworldOriginsMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        // JLF's MixTargetFinder replaces BC's findAttackTargetResult with an
        // implementation that calls Objects.requireNonNull on an attribute modifier
        // that may not exist, causing NPE for any player without JLF's reach modifier.
        if ("com.seniors.justlevelingfork.mixin.MixTargetFinder".equals(mixinClassName)) {
            return true;
        }
        // JLF's MixPlayer overrides getMaxAirSupply without calling super, breaking
        // all other @ModifyReturnValue chains on Entity.getMaxAirSupply (e.g. Origins).
        // Reimplemented in PlayerMaxAirSupplyMixin.
        if ("com.seniors.justlevelingfork.mixin.MixPlayer".equals(mixinClassName)) {
            return true;
        }
        // Origins' SelectionInvulnerabilityMixin makes a player invulnerable whenever any layer
        // is unchosen, an ambiguous derived state that can persist un-tracked. Replaced by
        // session-keyed invulnerability in ForgeEvents (invulnerable iff a SelectionSession is open).
        if ("io.github.apace100.origins.mixin.SelectionInvulnerabilityMixin".equals(mixinClassName)) {
            return true;
        }
        return false;
    }
}
