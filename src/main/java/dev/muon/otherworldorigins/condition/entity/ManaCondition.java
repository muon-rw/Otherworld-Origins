package dev.muon.otherworldorigins.condition.entity;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.util.Comparison;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ManaCondition {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (entity instanceof Player player) {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            int mana = (int) magicData.getMana();
            int compareTo = data.getInt("compare_to");
            Comparison comparison = data.get("comparison");
            return comparison.compare(mana, compareTo);
        }
        return false;
    }

    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                OtherworldOrigins.loc("player_mana"),
                new SerializableData()
                        .add("compare_to", SerializableDataTypes.INT, 0)
                        .add("comparison", ApoliDataTypes.COMPARISON, Comparison.GREATER_THAN_OR_EQUAL),
                ManaCondition::condition
        );
    }
}
