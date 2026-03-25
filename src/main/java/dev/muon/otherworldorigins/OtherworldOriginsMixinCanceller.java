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
        return false;
    }
}
