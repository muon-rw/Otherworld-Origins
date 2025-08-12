package dev.muon.otherworldorigins.condition.entity;

import io.github.edwinmindcraft.apoli.common.condition.entity.IntComparingEntityCondition;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ManaCondition extends IntComparingEntityCondition {

    public ManaCondition() {
        super(ManaCondition::getPlayerMana);
    }

    private static int getPlayerMana(Entity entity) {
        if (entity instanceof Player player) {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            return (int) magicData.getMana();
        }
        return 0;
    }
}