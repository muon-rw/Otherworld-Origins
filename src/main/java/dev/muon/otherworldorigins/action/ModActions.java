package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.core.Registry;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

public class ModActions {
    public static void register() {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            register(CastSpellAction.getFactory());
        }
        register(ClearNegativeEffectsAction.getFactory());
        register(ResetEnchantmentSeedAction.getFactory());
    }

    public static ActionFactory<Entity> register(ActionFactory<Entity> actionFactory) {
        return Registry.register(ApoliRegistries.ENTITY_ACTION, actionFactory.getSerializerId(), actionFactory);
    }
}
