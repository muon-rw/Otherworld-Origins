package dev.muon.otherworldorigins.condition;

import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.common.condition.entity.IntComparingEntityCondition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerLevelCondition extends IntComparingEntityCondition {

    public PlayerLevelCondition() {
        super(PlayerLevelCondition::getPlayerLevel);
    }

    private static int getPlayerLevel(Entity entity) {
        if (entity instanceof Player player) {
            return LevelingUtils.getPlayerLevel(player);
        }
        return 0;
    }
}