package dev.muon.raven_dnd_origins.school;

import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

import java.util.function.DoubleSupplier;

/**
 * A school whose power attribute is weapon damage rather than a base-1 multiplier: power is the damage relative to a
 * reference weapon, pulled toward 1 by the configured weapon weight.
 */
public class WeaponSchoolType extends SchoolType {
    private final Holder<Attribute> damageAttribute;
    private final DoubleSupplier referenceDamage;

    public WeaponSchoolType(ResourceLocation id, TagKey<Item> focus, Component displayName, Holder<Attribute> damageAttribute,
                            Holder<SoundEvent> castSound, ResourceKey<DamageType> damageType, DoubleSupplier referenceDamage) {
        super(id, focus, displayName, damageAttribute, Attributes.ARMOR, castSound, damageType);
        this.damageAttribute = damageAttribute;
        this.referenceDamage = referenceDamage;
    }

    @Override
    public double getPowerFor(LivingEntity livingEntity) {
        if (!livingEntity.getAttributes().hasAttribute(this.damageAttribute)) {
            return 1;
        }
        double ratio = livingEntity.getAttributeValue(this.damageAttribute) / this.referenceDamage.getAsDouble();
        return 1 + RavenDndOriginsConfig.weaponSchoolWeight() * (ratio - 1);
    }

    // Armor already reduces this school's damage types; Iron's would apply it a second time as 2 - armor
    @Override
    public double getResistanceFor(LivingEntity livingEntity) {
        return 1;
    }
}
