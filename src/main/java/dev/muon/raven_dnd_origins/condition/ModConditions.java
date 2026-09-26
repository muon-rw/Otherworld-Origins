package dev.muon.raven_dnd_origins.condition;

import com.mojang.serialization.MapCodec;
import dev.muon.raven_core.weapon.FistWeapons;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.condition.block.PlantableBlockCondition;
import dev.muon.raven_dnd_origins.condition.entity.AnyOnLayerCondition;
import dev.muon.raven_dnd_origins.condition.entity.HasSchoolAccessCondition;
import dev.muon.raven_dnd_origins.condition.entity.HasSpellRecastCondition;
import dev.muon.raven_dnd_origins.condition.entity.HasSkillCondition;
import dev.muon.raven_dnd_origins.condition.entity.LeveledChanceCondition;
import dev.muon.raven_dnd_origins.condition.entity.ManaCondition;
import dev.muon.raven_dnd_origins.condition.entity.PlayerLevelCondition;
import dev.muon.raven_dnd_origins.condition.entity.VelocityCondition;
import dev.muon.raven_dnd_origins.util.ExperimentalElixirLogic;
import net.minecraft.world.entity.LivingEntity;
import dev.muon.raven_dnd_origins.util.LootReforgeLogic;
import dev.muon.raven_dnd_origins.util.SoulOfArtificeLogic;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.item.CastingItem;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.ModList;

public class ModConditions {

    /**Block*/
    public static final ResourceLocation PLANTABLE = RavenDndOrigins.loc("plantable");

    /**Entity*/
    public static final ResourceLocation IS_ARROW = RavenDndOrigins.loc("is_arrow");
    public static final ResourceLocation CREATIVE_MODE = RavenDndOrigins.loc("creative_mode");
    public static final ResourceLocation IS_FRIENDLY = RavenDndOrigins.loc("is_friendly");
    public static final ResourceLocation PLAYER_LEVEL = RavenDndOrigins.loc("player_level");
    public static final ResourceLocation ANY_ON_LAYER = RavenDndOrigins.loc("any_on_layer");
    public static final ResourceLocation PLAYER_MANA = RavenDndOrigins.loc("player_mana");
    public static final ResourceLocation HAS_SKILL = RavenDndOrigins.loc("has_skill");
    public static final ResourceLocation HAS_SCHOOL_ACCESS = RavenDndOrigins.loc("has_school_access");
    public static final ResourceLocation HAS_SPELL_RECAST = RavenDndOrigins.loc("has_spell_recast");
    public static final ResourceLocation LEVELED_CHANCE = RavenDndOrigins.loc("leveled_chance");
    public static final ResourceLocation VELOCITY = RavenDndOrigins.loc("velocity");
    /** True when the entity is held by Iron's Spellbooks {@link RootEntity} (nature Root spell). */
    public static final ResourceLocation IS_ROOTED = RavenDndOrigins.loc("is_rooted");
    /** Main hand holds an Experimental Elixir reagent: empty bottle, water bottle or awkward potion. */
    public static final ResourceLocation HOLDING_EXPERIMENTAL_ELIXIR_REAGENT = RavenDndOrigins.loc("holding_experimental_elixir_reagent");

    /**Bientity*/
    public static final ResourceLocation IS_ALLIED = RavenDndOrigins.loc("allied");

    /**Item*/
    public static final ResourceLocation IS_MELEE_WEAPON = RavenDndOrigins.loc("is_melee_weapon");
    public static final ResourceLocation IS_BOW = RavenDndOrigins.loc("is_bow");
    public static final ResourceLocation IS_CROSSBOW = RavenDndOrigins.loc("is_crossbow");
    public static final ResourceLocation IS_ARMOR = RavenDndOrigins.loc("is_armor");
    public static final ResourceLocation IS_ONE_HANDED = RavenDndOrigins.loc("is_one_handed");
    public static final ResourceLocation IS_TWO_HANDED = RavenDndOrigins.loc("is_two_handed");
    public static final ResourceLocation CAN_CAST = RavenDndOrigins.loc("can_cast");
    public static final ResourceLocation IS_SWORD = RavenDndOrigins.loc("is_sword");
    public static final ResourceLocation IS_STAFF = RavenDndOrigins.loc("is_staff");
    /** True when {@link dev.muon.raven_dnd_origins.action.item.SoulOfArtificeItemAction} would successfully roll a bonus affix. */
    public static final ResourceLocation AFFIXABLE = RavenDndOrigins.loc("affixable");
    /** Main-hand items that {@link dev.muon.raven_dnd_origins.util.LootReforgeLogic#tryReforgeMainHand} may roll (Apotheosis loot category, etc.). */
    public static final ResourceLocation REFORGE_ELIGIBLE = RavenDndOrigins.loc("reforge_eligible");
    public static final ResourceLocation IS_FIST_WEAPON = RavenDndOrigins.loc("is_fist_weapon");
    public static final ResourceLocation IS_TOOL = RavenDndOrigins.loc("is_tool");
    public static final ResourceLocation IS_GOLDEN_ARMOR = RavenDndOrigins.loc("is_golden_armor");
    public static final ResourceLocation IS_GOLDEN_WEAPON = RavenDndOrigins.loc("is_golden_weapon");
    public static final ResourceLocation IS_GOLDEN_TOOL = RavenDndOrigins.loc("is_golden_tool");

    public static void register() {
        ConditionTypes.BLOCK.register(PLANTABLE, new PlantableBlockCondition());

        ConditionTypes.ENTITY.register(IS_ARROW, new IsArrowCondition());
        ConditionTypes.ENTITY.register(CREATIVE_MODE, new CreativeModeCondition());
        ConditionTypes.ENTITY.register(IS_FRIENDLY, new IsFriendlyCondition());
        ConditionTypes.ENTITY.register(PLAYER_LEVEL, new PlayerLevelCondition());
        ConditionTypes.ENTITY.register(ANY_ON_LAYER, new AnyOnLayerCondition());
        ConditionTypes.ENTITY.register(PLAYER_MANA, new ManaCondition());
        ConditionTypes.ENTITY.register(HAS_SKILL, new HasSkillCondition());
        ConditionTypes.ENTITY.register(HAS_SCHOOL_ACCESS, new HasSchoolAccessCondition());
        ConditionTypes.ENTITY.register(HAS_SPELL_RECAST, new HasSpellRecastCondition());
        ConditionTypes.ENTITY.register(LEVELED_CHANCE, new LeveledChanceCondition());
        ConditionTypes.ENTITY.register(VELOCITY, new VelocityCondition());
        ConditionTypes.ENTITY.register(IS_ROOTED, new IsRootedCondition());
        ConditionTypes.ENTITY.register(HOLDING_EXPERIMENTAL_ELIXIR_REAGENT, new HoldingExperimentalElixirReagentCondition());

        ConditionTypes.BI_ENTITY.register(IS_ALLIED, new IsAlliedCondition());

        ConditionTypes.ITEM.register(IS_MELEE_WEAPON, new IsMeleeWeaponCondition());
        ConditionTypes.ITEM.register(IS_BOW, new IsBowCondition());
        ConditionTypes.ITEM.register(IS_CROSSBOW, new IsCrossbowCondition());
        ConditionTypes.ITEM.register(IS_ARMOR, new IsArmorCondition());
        ConditionTypes.ITEM.register(IS_ONE_HANDED, new IsOneHandedCondition());
        ConditionTypes.ITEM.register(IS_TWO_HANDED, new IsTwoHandedCondition());
        ConditionTypes.ITEM.register(CAN_CAST, new CanCastCondition());
        ConditionTypes.ITEM.register(IS_SWORD, new IsSwordCondition());
        ConditionTypes.ITEM.register(IS_STAFF, new IsStaffCondition());
        ConditionTypes.ITEM.register(AFFIXABLE, new AffixableCondition());
        ConditionTypes.ITEM.register(REFORGE_ELIGIBLE, new ReforgeEligibleCondition());
        ConditionTypes.ITEM.register(IS_FIST_WEAPON, new IsFistWeaponCondition());
        ConditionTypes.ITEM.register(IS_TOOL, new IsToolCondition());
        ConditionTypes.ITEM.register(IS_GOLDEN_ARMOR, new IsGoldenArmorCondition());
        ConditionTypes.ITEM.register(IS_GOLDEN_WEAPON, new IsGoldenWeaponCondition());
        ConditionTypes.ITEM.register(IS_GOLDEN_TOOL, new IsGoldenToolCondition());
    }

    private static boolean hasPositiveAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double[] totalDamage = {0.0};
        modifiers.forEach(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value()) {
                totalDamage[0] += modifier.amount();
            }
        });
        return totalDamage[0] > 0;
    }

    private static String pathOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static final class IsArrowCondition implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return ctx.raw() instanceof AbstractArrow;
        }
    }

    private static final class CreativeModeCondition implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return ctx.raw() instanceof Player player && player.getAbilities().instabuild;
        }
    }

    private static final class IsFriendlyCondition implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return ctx.raw().getType().getCategory().isFriendly();
        }
    }

    private static final class IsRootedCondition implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return ctx.raw().getVehicle() instanceof RootEntity;
        }
    }

    private static final class HoldingExperimentalElixirReagentCondition implements ConditionType<EntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, EntityCtx ctx) {
            return ctx.raw() instanceof LivingEntity living && ExperimentalElixirLogic.isReagent(living.getMainHandItem());
        }
    }

    private static final class IsAlliedCondition implements ConditionType<BiEntityCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, BiEntityCtx ctx) {
            return ctx.actor().isAlliedTo(ctx.target());
        }
    }

    private static final class IsMeleeWeaponCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            return stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                    || (stack.getItem() instanceof DiggerItem && hasPositiveAttackDamage(stack));
        }
    }

    private static final class IsBowCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return ctx.stack().getItem() instanceof BowItem;
        }
    }

    private static final class IsCrossbowCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return ctx.stack().getItem() instanceof CrossbowItem;
        }
    }

    private static final class IsArmorCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return ctx.stack().getItem() instanceof ArmorItem;
        }
    }

    private static final class IsOneHandedCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            if (ModList.get().isLoaded("bettercombat")) {
                WeaponAttributes attributes = WeaponRegistry.getAttributes(ctx.stack());
                return attributes != null && !attributes.isTwoHanded();
            }
            return true;
        }
    }

    private static final class IsTwoHandedCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            if (ModList.get().isLoaded("bettercombat")) {
                WeaponAttributes attributes = WeaponRegistry.getAttributes(ctx.stack());
                return attributes != null && attributes.isTwoHanded();
            }
            return false;
        }
    }

    private static final class CanCastCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return ctx.stack().getItem() instanceof CastingItem;
        }
    }

    private static final class IsSwordCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            return (stack.getItem() instanceof SwordItem || stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE))
                    && pathOf(stack).contains("sword");
        }
    }

    private static final class IsStaffCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return pathOf(ctx.stack()).contains("staff");
        }
    }

    private static final class AffixableCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return SoulOfArtificeLogic.canApplyBonusAffix(ctx.stack());
        }
    }

    private static final class ReforgeEligibleCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return LootReforgeLogic.isReforgeEligible(ctx.stack());
        }
    }

    private static final class IsFistWeaponCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            return FistWeapons.isFistWeapon(ctx.stack());
        }
    }

    private static final class IsToolCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            return stack.getItem() instanceof DiggerItem || stack.is(ItemTags.MINING_ENCHANTABLE);
        }
    }

    private static final class IsGoldenArmorCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            String path = pathOf(stack);
            return stack.getItem() instanceof ArmorItem && (path.contains("gold") || path.contains("gilded"));
        }
    }

    private static final class IsGoldenWeaponCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            String path = pathOf(stack);
            return stack.is(ItemTags.SHARP_WEAPON_ENCHANTABLE) && (path.contains("gold") || path.contains("gilded"));
        }
    }

    private static final class IsGoldenToolCondition implements ConditionType<ItemCtx, EmptyCfg> {
        @Override
        public MapCodec<EmptyCfg> codec() {
            return MapCodec.unit(EmptyCfg.INSTANCE);
        }

        @Override
        public boolean test(EmptyCfg cfg, ItemCtx ctx) {
            ItemStack stack = ctx.stack();
            String path = pathOf(stack);
            return stack.getItem() instanceof DiggerItem && (path.contains("gold") || path.contains("gilded"));
        }
    }
}
