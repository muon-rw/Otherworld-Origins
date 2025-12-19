package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ResetEnchantmentSeedAction {

    public static void action(SerializableData.Instance data, Entity entity) {
        if (entity instanceof Player player && player instanceof IEnchantmentSeedResettable resettable) {
            resettable.otherworld$resetEnchantmentSeed();
        }
    }

    public static ActionFactory<Entity> getFactory() {
        return new ActionFactory<>(
                OtherworldOrigins.loc("reset_enchantment_seed"),
                new SerializableData(),
                ResetEnchantmentSeedAction::action
        );
    }
}
