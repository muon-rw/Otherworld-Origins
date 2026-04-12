package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.configuration.HolderConfiguration;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBlockCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModPowers {
    public static final DeferredRegister<PowerFactory<?>> POWER_FACTORIES = DeferredRegister.create(ApoliRegistries.POWER_FACTORY_KEY, OtherworldOrigins.MODID);

    public static final RegistryObject<PowerFactory<InnateAptitudeBonusPower.Configuration>> INNATE_APTITUDE_BONUS = POWER_FACTORIES.register("innate_aptitude_bonus", InnateAptitudeBonusPower::new);

    public static final RegistryObject<PowerFactory<HolderConfiguration<ConfiguredEntityAction<?, ?>>>> ACTION_ON_LEVELUP = POWER_FACTORIES.register("action_on_levelup", ActionOnLevelupPower::new);
    public static final RegistryObject<PowerFactory<TradeDiscountPower.Configuration>> TRADE_DISCOUNT = POWER_FACTORIES.register("trade_discount", TradeDiscountPower::new);
    public static final RegistryObject<PowerFactory<ModifyThirstExhaustionPower.Configuration>> MODIFY_THIRST_EXHAUSTION = POWER_FACTORIES.register("modify_thirst_exhaustion", ModifyThirstExhaustionPower::new);
    public static final RegistryObject<PowerFactory<CharismaPower.Configuration>> CHARISMA = POWER_FACTORIES.register("charisma", CharismaPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> HUNGER_IMMUNITY = POWER_FACTORIES.register("hunger_immunity", HungerImmunityPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> ELDRITCH_KNOWLEDGE = POWER_FACTORIES.register("eldritch_knowledge", EldritchKnowledgePower::new);
    public static final RegistryObject<PowerFactory<LeveledAttributePower.Configuration>> LEVELED_ATTRIBUTE = POWER_FACTORIES.register("leveled_attribute", LeveledAttributePower::new);
    public static final RegistryObject<PowerFactory<ModifyStatusEffectCategoryPower.Configuration>> MODIFY_STATUS_EFFECT_CATEGORY = POWER_FACTORIES.register("modify_status_effect_category", () -> ModifyStatusEffectCategoryPower.INSTANCE);
    public static final RegistryObject<PowerFactory<ModifyEnchantmentCostPower.Configuration>> MODIFY_ENCHANTMENT_COST = POWER_FACTORIES.register("modify_enchantment_cost", ModifyEnchantmentCostPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> PREVENT_CRITICAL_HIT = POWER_FACTORIES.register("prevent_critical_hit", PreventCriticalHitPower::new);
    public static final RegistryObject<PowerFactory<ModifyCriticalHitPower.Configuration>> MODIFY_CRITICAL_HIT = POWER_FACTORIES.register("modify_critical_hit", ModifyCriticalHitPower::new);
    public static final RegistryObject<PowerFactory<GoldDurabilityPower.Configuration>> GOLD_DURABILITY = POWER_FACTORIES.register("gold_durability", GoldDurabilityPower::new);
    public static final RegistryObject<LeveledResourcePower> LEVELED_RESOURCE = POWER_FACTORIES.register("leveled_resource", LeveledResourcePower::new);
    public static final RegistryObject<ModifyDamageTakenDirectPower> MODIFY_DAMAGE_TAKEN = POWER_FACTORIES.register("modify_damage_taken", ModifyDamageTakenDirectPower::new);
    public static final RegistryObject<HealFromDamagePower> HEAL_FROM_DAMAGE = POWER_FACTORIES.register("heal_from_damage", HealFromDamagePower::new);
    public static final RegistryObject<DeflectProjectilePower> DEFLECT_PROJECTILE = POWER_FACTORIES.register("deflect_projectile", DeflectProjectilePower::new);
    public static final RegistryObject<PlayerLevelPower> PLAYER_LEVEL = POWER_FACTORIES.register("player_level", PlayerLevelPower::new);
    public static final RegistryObject<PowerFactory<SpellImmunityPower.Configuration>> SPELL_IMMUNITY = POWER_FACTORIES.register("spell_immunity", SpellImmunityPower::new);
    public static final RegistryObject<PowerFactory<HolderConfiguration<ConfiguredBlockCondition<?, ?>>>> PREVENT_BLOCK_SLOWDOWN = POWER_FACTORIES.register("prevent_block_slowdown", PreventBlockSlowdownPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> PREVENT_REPAIR_PENALTY = POWER_FACTORIES.register("prevent_repair_penalty", PreventRepairPenaltyPower::new);
    public static final RegistryObject<PowerFactory<EnhancedRepairPower.Configuration>> ENHANCED_REPAIR = POWER_FACTORIES.register("enhanced_repair", EnhancedRepairPower::new);
    public static final RegistryObject<PowerFactory<AllowedSpellsPower.Configuration>> ALLOWED_SPELLS = POWER_FACTORIES.register("allowed_spells", AllowedSpellsPower::new);
    public static final RegistryObject<PowerFactory<DirectionalTeleportPower.Configuration>> DIRECTIONAL_TELEPORT = POWER_FACTORIES.register("directional_teleport", DirectionalTeleportPower::new);
    public static final RegistryObject<PowerFactory<ShapeshiftPower.Configuration>> SHAPESHIFT = POWER_FACTORIES.register("shapeshift", ShapeshiftPower::new);
    public static final RegistryObject<ActionOnAttackPower> ACTION_ON_ATTACK = POWER_FACTORIES.register("action_on_attack", ActionOnAttackPower::new);
    public static final RegistryObject<PowerFactory<ModifyMaxAirSupplyPower.Configuration>> MODIFY_MAX_AIR_SUPPLY = POWER_FACTORIES.register("modify_max_air_supply", ModifyMaxAirSupplyPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> SUFFOCATION_IMMUNITY = POWER_FACTORIES.register("suffocation_immunity", SuffocationImmunityPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> THIRST_IMMUNITY = POWER_FACTORIES.register("thirst_immunity", ThirstImmunityPower::new);
    public static final RegistryObject<PowerFactory<EffectCategoryImmunityPower.Configuration>> EFFECT_CATEGORY_IMMUNITY = POWER_FACTORIES.register("effect_category_immunity", EffectCategoryImmunityPower::new);
    public static final RegistryObject<PowerFactory<NoConfiguration>> WALK_ON_POWDER_SNOW = POWER_FACTORIES.register("walk_on_powder_snow", WalkOnPowderSnowPower::new);
    public static final RegistryObject<PowerFactory<JumpCooldownPower.Configuration>> JUMP_COOLDOWN = POWER_FACTORIES.register("jump_cooldown", JumpCooldownPower::new);
    public static final RegistryObject<PowerFactory<MobsIgnorePower.Configuration>> MOBS_IGNORE = POWER_FACTORIES.register("mobs_ignore", MobsIgnorePower::new);
    public static final RegistryObject<PowerFactory<InspirationPower.Configuration>> INSPIRATION = POWER_FACTORIES.register("inspiration", InspirationPower::new);
    public static final RegistryObject<PowerFactory<ActionOnSpellCastPower.Configuration>> ACTION_ON_SPELL_CAST = POWER_FACTORIES.register("action_on_spell_cast", ActionOnSpellCastPower::new);
    public static final RegistryObject<PowerFactory<RecastSpellPower.Configuration>> RECAST_SPELL = POWER_FACTORIES.register("recast_spell", RecastSpellPower::new);


    private static <T extends PowerFactory<?>> RegistryObject<T> registerConditional(String name, Supplier<T> factory, String modId) {
        if (ModList.get().isLoaded(modId)) {
            return POWER_FACTORIES.register(name, factory);
        }
        return null;
    }

    public static void register(IEventBus eventBus) {
        POWER_FACTORIES.register(eventBus);
    }
}