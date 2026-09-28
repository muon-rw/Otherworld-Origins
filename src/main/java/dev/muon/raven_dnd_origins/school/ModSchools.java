package dev.muon.raven_dnd_origins.school;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.util.TagRefs;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static io.redspace.ironsspellbooks.api.registry.SchoolRegistry.SCHOOL_REGISTRY_KEY;

public class ModSchools {
    private static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SCHOOL_REGISTRY_KEY, RavenDndOrigins.MODID);

    public static void register(IEventBus eventBus) {
        SCHOOLS.register(eventBus);
    }

    private static DeferredHolder<SchoolType, SchoolType> registerSchool(SchoolType type) {
        return SCHOOLS.register(type.getId().getPath(), () -> type);
    }

    private static Holder<Attribute> archeryPowerAttribute() {
        return BuiltInRegistries.ATTRIBUTE.getHolder(RANGED_WEAPON_DAMAGE)
                .<Holder<Attribute>>map(holder -> holder)
                .orElse(ALObjects.Attributes.ARROW_DAMAGE);
    }

    public static final ResourceLocation MARTIAL_RESOURCE = RavenDndOrigins.loc("martial");
    public static final ResourceLocation ARCHERY_RESOURCE = RavenDndOrigins.loc("archery");
    private static final ResourceLocation RANGED_WEAPON_DAMAGE = ResourceLocation.fromNamespaceAndPath("ranged_weapon", "damage");

    private static final Holder<Attribute> ARCHERY_POWER = archeryPowerAttribute();

    // Apothic arrow damage, the fallback, is a percent attribute with base 1
    private static double archeryReferenceDamage() {
        return ARCHERY_POWER.is(RANGED_WEAPON_DAMAGE) ? RavenDndOriginsConfig.archeryReferenceDamage() : 1;
    }

    public static final DeferredHolder<SchoolType, SchoolType> MARTIAL = registerSchool(new WeaponSchoolType(
            MARTIAL_RESOURCE,
            TagRefs.MARTIAL_FOCUS,
            Component.translatable("school.raven_dnd_origins.martial").withStyle(Style.EMPTY.withColor(0xB33A3A)),
            Attributes.ATTACK_DAMAGE,
            SoundRegistry.EVOCATION_CAST,
            DamageTypes.PLAYER_ATTACK,
            RavenDndOriginsConfig::martialReferenceDamage
    ));

    public static final DeferredHolder<SchoolType, SchoolType> ARCHERY = registerSchool(new WeaponSchoolType(
            ARCHERY_RESOURCE,
            TagRefs.ARCHERY_FOCUS,
            Component.translatable("school.raven_dnd_origins.archery").withStyle(Style.EMPTY.withColor(0x5E9E3A)),
            ARCHERY_POWER,
            SoundRegistry.EVOCATION_CAST,
            DamageTypes.ARROW,
            ModSchools::archeryReferenceDamage
    ));
}
