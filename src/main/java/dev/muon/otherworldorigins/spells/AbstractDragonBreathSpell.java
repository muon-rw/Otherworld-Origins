package dev.muon.otherworldorigins.spells;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.dragon_breath.DragonBreathProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.EntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Optional;

public abstract class AbstractDragonBreathSpell extends AbstractSpell {
    private final ResourceLocation spellId;

    public AbstractDragonBreathSpell(String spellName) {
        this.spellId = new ResourceLocation(OtherworldOrigins.MODID, spellName);
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 100;
        this.baseManaCost = 5;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)));
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(getSchoolType().getId())
                .setMaxLevel(10)
                .setCooldownSeconds(12)
                .build();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_GROWL);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_BREATH_LOOP.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_SPIT_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.SPIT_FINISH_ANIMATION;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.isCasting() && playerMagicData.getCastingSpell().getSpell().equals(this)
                && playerMagicData.getAdditionalCastData() instanceof EntityCastData entityCastData
                && entityCastData.getCastingEntity() instanceof AbstractConeProjectile cone) {
            cone.setDealDamageActive();
        } else {
            AbstractConeProjectile coneProjectile = createConeProjectile(world, entity);
            coneProjectile.setPos(entity.position().add(0, entity.getEyeHeight() * .7, 0));
            coneProjectile.setDamage(getDamage(spellLevel, entity));
            world.addFreshEntity(coneProjectile);

            playerMagicData.setAdditionalCastData(new EntityCastData(coneProjectile));
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
    protected abstract AbstractConeProjectile createConeProjectile(Level world, LivingEntity entity);


    public float getDamage(int spellLevel, LivingEntity caster) {
        return 1 + getSpellPower(spellLevel, caster) * .75f;
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > (10 * 10) * 1.2;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    public abstract SchoolType getSchoolType();
    protected SchoolType getSchoolTypeFromRegistry(RegistryObject<SchoolType> registryObject) {
        return registryObject.get();
    }
}