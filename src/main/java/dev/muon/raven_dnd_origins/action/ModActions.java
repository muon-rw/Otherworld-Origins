package dev.muon.raven_dnd_origins.action;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.action.bientity.*;
import dev.muon.raven_dnd_origins.action.item.SoulOfArtificeItemAction;
import dev.muon.raven_dnd_origins.action.entity.*;
import dev.overgrown.apoli.action.ActionTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public class ModActions {
    public static final ResourceLocation CAST_SPELL = RavenDndOrigins.loc("cast_spell");
    public static final ResourceLocation RESTORE_MANA = RavenDndOrigins.loc("restore_mana");
    public static final ResourceLocation RESET_SPELL_COOLDOWNS = RavenDndOrigins.loc("reset_spell_cooldowns");
    public static final ResourceLocation REDUCE_SPELL_COOLDOWNS = RavenDndOrigins.loc("reduce_spell_cooldowns");
    public static final ResourceLocation CLEAR_NEGATIVE_EFFECTS = RavenDndOrigins.loc("clear_negative_effects");
    public static final ResourceLocation RESET_ENCHANTMENT_SEED = RavenDndOrigins.loc("reset_enchantment_seed");
    public static final ResourceLocation LEVELED_HEAL = RavenDndOrigins.loc("leveled_heal");
    public static final ResourceLocation RESOURCE_HEAL = RavenDndOrigins.loc("resource_heal");
    public static final ResourceLocation PLAY_PLAYER_ANIMATION = RavenDndOrigins.loc("play_player_animation");
    public static final ResourceLocation PROMPT_LAYER_SELECTION = RavenDndOrigins.loc("prompt_layer_selection");
    public static final ResourceLocation ACTION_BAR_MESSAGE = RavenDndOrigins.loc("action_bar_message");
    public static final ResourceLocation APPLY_EFFECT_STACKING = RavenDndOrigins.loc("apply_effect_stacking");
    public static final ResourceLocation GRANT_ABSORPTION_HEARTS = RavenDndOrigins.loc("grant_absorption_hearts");
    public static final ResourceLocation APPLY_LEVELED_EFFECT = RavenDndOrigins.loc("apply_leveled_effect");
    public static final ResourceLocation AREA_OF_EFFECT_SEQUENTIAL = RavenDndOrigins.loc("area_of_effect_sequential");
    public static final ResourceLocation REFORGE_HELD_ITEM = RavenDndOrigins.loc("reforge_held_item");
    public static final ResourceLocation SET_VELOCITY = RavenDndOrigins.loc("set_velocity");

    public static final ResourceLocation TAME = RavenDndOrigins.loc("tame");
    public static final ResourceLocation LEVELED_HEAL_BIENTITY = RavenDndOrigins.loc("leveled_heal");
    public static final ResourceLocation LEVELED_DAMAGE_BIENTITY = RavenDndOrigins.loc("leveled_damage");
    public static final ResourceLocation RESOURCE_HEAL_BIENTITY = RavenDndOrigins.loc("resource_heal");
    public static final ResourceLocation RAYCAST_BETWEEN = RavenDndOrigins.loc("raycast_between");
    public static final ResourceLocation APPLY_LEVELED_EFFECT_BIENTITY = RavenDndOrigins.loc("apply_leveled_effect");
    public static final ResourceLocation ATTACK = RavenDndOrigins.loc("attack");
    public static final ResourceLocation CAST_SPELL_BIENTITY = RavenDndOrigins.loc("cast_spell");
    public static final ResourceLocation EXPERIMENTAL_ELIXIR_BREW = RavenDndOrigins.loc("experimental_elixir_brew");
    public static final ResourceLocation SPELL_THIEF = RavenDndOrigins.loc("spell_thief");

    public static final ResourceLocation SOUL_OF_ARTIFICE = RavenDndOrigins.loc("soul_of_artifice");

    public static void register() {
        boolean ironsSpellbooks = ModList.get().isLoaded("irons_spellbooks");

        if (ironsSpellbooks) {
            ActionTypes.ENTITY.register(CAST_SPELL, new CastSpellAction());
            ActionTypes.ENTITY.register(RESTORE_MANA, new RestoreManaAction());
            ActionTypes.ENTITY.register(RESET_SPELL_COOLDOWNS, new ResetSpellCooldownsAction());
            ActionTypes.ENTITY.register(REDUCE_SPELL_COOLDOWNS, new ReduceSpellCooldownsAction());
        }
        ActionTypes.ENTITY.register(CLEAR_NEGATIVE_EFFECTS, new ClearNegativeEffectsAction());
        ActionTypes.ENTITY.register(RESET_ENCHANTMENT_SEED, new ResetEnchantmentSeedAction());
        ActionTypes.ENTITY.register(EXPERIMENTAL_ELIXIR_BREW, new ExperimentalElixirBrewAction());
        ActionTypes.ENTITY.register(LEVELED_HEAL, new LeveledHealAction());
        ActionTypes.ENTITY.register(RESOURCE_HEAL, new ResourceHealAction());
        ActionTypes.ENTITY.register(PLAY_PLAYER_ANIMATION, new PlayPlayerAnimationAction());
        ActionTypes.ENTITY.register(PROMPT_LAYER_SELECTION, new PromptLayerSelectionAction());
        ActionTypes.ENTITY.register(ACTION_BAR_MESSAGE, new ActionBarMessageAction());
        ActionTypes.ENTITY.register(APPLY_EFFECT_STACKING, new ApplyEffectStackingAction());
        ActionTypes.ENTITY.register(GRANT_ABSORPTION_HEARTS, new GrantAbsorptionHeartsAction());
        ActionTypes.ENTITY.register(APPLY_LEVELED_EFFECT, new ApplyLeveledEffectAction());
        ActionTypes.ENTITY.register(AREA_OF_EFFECT_SEQUENTIAL, new AreaOfEffectSequentialAction());
        ActionTypes.ENTITY.register(REFORGE_HELD_ITEM, new ReforgeHeldItemAction());
        ActionTypes.ENTITY.register(SET_VELOCITY, new SetVelocityAction());

        ActionTypes.BI_ENTITY.register(TAME, new TameAction());
        ActionTypes.BI_ENTITY.register(LEVELED_HEAL_BIENTITY, new LeveledHealBientityAction());
        ActionTypes.BI_ENTITY.register(LEVELED_DAMAGE_BIENTITY, new LeveledDamageBientityAction());
        ActionTypes.BI_ENTITY.register(RESOURCE_HEAL_BIENTITY, new ResourceHealBientityAction());
        ActionTypes.BI_ENTITY.register(RAYCAST_BETWEEN, new RaycastBetweenAction());
        ActionTypes.BI_ENTITY.register(APPLY_LEVELED_EFFECT_BIENTITY, new ApplyLeveledEffectBientityAction());
        ActionTypes.BI_ENTITY.register(ATTACK, new AttackAction());
        if (ironsSpellbooks) {
            ActionTypes.BI_ENTITY.register(CAST_SPELL_BIENTITY, new CastSpellBientityAction());
            ActionTypes.BI_ENTITY.register(SPELL_THIEF, new SpellThiefBientityAction());
        }

        ActionTypes.ITEM.register(SOUL_OF_ARTIFICE, new SoulOfArtificeItemAction());
    }
}
