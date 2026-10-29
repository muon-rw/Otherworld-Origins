package dev.muon.raven_dnd_origins.mixin.compat.rpg_villagers;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.CharismaPower;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.rpgvillagers.village.Standing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Standing.class, remap = false)
public class StandingMixin {

    // RPG Villagers' standing thresholds are 1 (known), 15 (trusted), and 45 (honoured),
    // so 0.25 Charisma makes strangers known and 0.5 gets most of the way to trusted.
    @Unique
    private static final int raven_dnd_origins$REPUTATION_PER_CHARISMA = 20;

    @ModifyReturnValue(method = "reputation", at = @At("RETURN"))
    private static int raven_dnd_origins$addCharismaToReputation(int reputation, @Local(argsOnly = true) Player player) {
        return reputation + Math.round(CharismaPower.totalCharisma(player) * raven_dnd_origins$REPUTATION_PER_CHARISMA);
    }
}
