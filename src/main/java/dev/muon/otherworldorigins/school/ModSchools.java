package dev.muon.otherworldorigins.school;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.TagRefs;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static io.redspace.ironsspellbooks.api.registry.SchoolRegistry.SCHOOL_REGISTRY_KEY;

public class ModSchools {
    private static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SCHOOL_REGISTRY_KEY, OtherworldOrigins.MODID);

    public static void register(IEventBus eventBus) {
        SCHOOLS.register(eventBus);
    }

    private static RegistryObject<SchoolType> registerSchool(SchoolType type) {
        return SCHOOLS.register(type.getId().getPath(), () -> type);
    }

    public static final ResourceLocation MARTIAL_RESOURCE = OtherworldOrigins.loc("martial");
    public static final ResourceLocation ARCHERY_RESOURCE = OtherworldOrigins.loc("archery");

    public static final RegistryObject<SchoolType> MARTIAL = registerSchool(new SchoolType(
                    MARTIAL_RESOURCE,
                    TagRefs.MARTIAL_FOCUS,
                    Component.translatable("school.otherworldorigins.martial").withStyle(Style.EMPTY.withColor(0x36156c)),
                    () -> Attributes.ATTACK_DAMAGE,
                    () -> Attributes.ARMOR,
                    SoundRegistry.EVOCATION_CAST,
                    DamageTypes.PLAYER_ATTACK
            ));

    public static final RegistryObject<SchoolType> ARCHERY = registerSchool(new SchoolType(
            ARCHERY_RESOURCE,
            TagRefs.ARCHERY_FOCUS,
            Component.translatable("school.otherworldorigins.archery").withStyle(Style.EMPTY.withColor(0x36156c)),
            ALObjects.Attributes.ARROW_DAMAGE,
            () -> Attributes.ARMOR,
            SoundRegistry.EVOCATION_CAST,
            DamageTypes.ARROW
    ));


}