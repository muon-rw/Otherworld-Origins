package dev.muon.otherworldorigins.condition.entity;

import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.common.condition.entity.IntComparingEntityCondition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Compares {@link LevelingUtils#getPlayerLevel} to {@code compare_to}. Non-players use {@code 0}.
 */
public class PlayerLevelCondition extends IntComparingEntityCondition {

    public PlayerLevelCondition() {
        super(PlayerLevelCondition::characterLevel);
    }

    private static int characterLevel(Entity entity) {
        if (entity instanceof Player player) {
            return LevelingUtils.getPlayerLevel(player);
        }
        return 0;
    }
}
