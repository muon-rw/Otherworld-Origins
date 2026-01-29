package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.ItemAction;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModActions {
    public static final DeferredRegister<EntityAction<?>> ENTITY_ACTIONS = DeferredRegister.create(ApoliRegistries.ENTITY_ACTION_KEY, OtherworldOrigins.MODID);
    public static final RegistryObject<CastSpellAction> CAST_SPELL = ModList.get().isLoaded("irons_spellbooks") ?
            ENTITY_ACTIONS.register("cast_spell", CastSpellAction::new) : null;
    public static final RegistryObject<ClearNegativeEffectsAction> CLEAR_NEGATIVE_EFFECTS = ENTITY_ACTIONS.register("clear_negative_effects", ClearNegativeEffectsAction::new);
    public static final RegistryObject<ResetEnchantmentSeedAction> RESET_ENCHANTMENT_SEED = ENTITY_ACTIONS.register("reset_enchantment_seed", ResetEnchantmentSeedAction::new);
    public static final RegistryObject<LeveledRestoreAction> LEVELED_RESTORE = ENTITY_ACTIONS.register("leveled_restore", LeveledRestoreAction::new);



    public static final DeferredRegister<BiEntityAction<?>> BIENTITY_ACTIONS = DeferredRegister.create(ApoliRegistries.BIENTITY_ACTION_KEY, OtherworldOrigins.MODID);
    public static final RegistryObject<TameAction> TAME = BIENTITY_ACTIONS.register("tame",
            () -> new TameAction(TameAction::tame));
    public static final RegistryObject<CastSpellBientityAction> CAST_SPELL_BIENTITY = ModList.get().isLoaded("irons_spellbooks") ?
            BIENTITY_ACTIONS.register("cast_spell", CastSpellBientityAction::new) : null;


    public static final DeferredRegister<ItemAction<?>> ITEM_ACTIONS = DeferredRegister.create(ApoliRegistries.ITEM_ACTION_KEY, OtherworldOrigins.MODID);


    public static void register(IEventBus eventBus) {
        ENTITY_ACTIONS.register(eventBus);
        BIENTITY_ACTIONS.register(eventBus);
        ITEM_ACTIONS.register(eventBus);
    }
}