package dev.muon.otherworldorigins.spells;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.dragon_breath.DragonBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.poison_breath.PoisonBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.cone_of_cold.ConeOfColdProjectile;
import io.redspace.ironsspellbooks.entity.spells.electrocute.ElectrocuteProjectile;
import io.redspace.ironsspellbooks.entity.spells.gust.GustCollider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DragonBreathSpells {
    public static class BlackDragonBreathSpell extends AbstractDragonBreathSpell {
        public BlackDragonBreathSpell() {
            super("black_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.NATURE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new PoisonBreathProjectile(world, entity);
        }
    }

    public static class BlueDragonBreathSpell extends AbstractDragonBreathSpell {
        public BlueDragonBreathSpell() {
            super("blue_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.LIGHTNING);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new ElectrocuteProjectile(world, entity);
        }
    }

    public static class BrassDragonBreathSpell extends AbstractDragonBreathSpell {
        public BrassDragonBreathSpell() {
            super("brass_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.FIRE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new FireBreathProjectile(world, entity);
        }
    }

    public static class BronzeDragonBreathSpell extends AbstractDragonBreathSpell {
        public BronzeDragonBreathSpell() {
            super("bronze_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.LIGHTNING);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new ElectrocuteProjectile(world, entity);
        }
    }

    public static class CopperDragonBreathSpell extends AbstractDragonBreathSpell {
        public CopperDragonBreathSpell() {
            super("copper_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.NATURE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new PoisonBreathProjectile(world, entity);
        }
    }

    public static class GoldDragonBreathSpell extends AbstractDragonBreathSpell {
        public GoldDragonBreathSpell() {
            super("gold_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.FIRE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new FireBreathProjectile(world, entity);
        }
    }

    public static class GreenDragonBreathSpell extends AbstractDragonBreathSpell {
        public GreenDragonBreathSpell() {
            super("green_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.NATURE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new PoisonBreathProjectile(world, entity);
        }
    }

    public static class RedDragonBreathSpell extends AbstractDragonBreathSpell {
        public RedDragonBreathSpell() {
            super("red_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.FIRE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new FireBreathProjectile(world, entity);
        }
    }

    public static class SilverDragonBreathSpell extends AbstractDragonBreathSpell {
        public SilverDragonBreathSpell() {
            super("silver_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.ICE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new ConeOfColdProjectile(world, entity);
        }
    }

    public static class WhiteDragonBreathSpell extends AbstractDragonBreathSpell {
        public WhiteDragonBreathSpell() {
            super("white_dragon_breath");
        }

        @Override
        public SchoolType getSchoolType() {
            return getSchoolTypeFromRegistry(SchoolRegistry.ICE);
        }

        @Override
        protected AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity) {
            return new ConeOfColdProjectile(world, entity);
        }
    }
}