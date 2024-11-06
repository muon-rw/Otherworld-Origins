package dev.muon.otherworldorigins.spells;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.entity.summons.SummonedIronGolem;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class SummonGolemSpell extends AbstractSpell {
    private final ResourceLocation spellId = OtherworldOrigins.loc("summon_golem");
    private final DefaultConfig defaultConfig;

    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.hp", new Object[]{this.getHealth(spellLevel, (LivingEntity)null)}), Component.translatable("ui.irons_spellbooks.damage", new Object[]{this.getDamage(spellLevel, (LivingEntity)null)}));
    }

    public SummonGolemSpell() {
        this.defaultConfig = (new DefaultConfig()).setMinRarity(SpellRarity.RARE).setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE).setMaxLevel(10).setCooldownSeconds(180.0).build();
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    public CastType getCastType() {
        return CastType.LONG;
    }

    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int summonTime = 12000;

        if (entity.hasEffect(ModEffects.GOLEM_TIMER.get())) {
            world.getEntitiesOfClass(SummonedIronGolem.class, entity.getBoundingBox().inflate(32.0D))
                    .stream()
                    .filter(golem -> golem.getSummoner() == entity)
                    .findFirst()
                    .ifPresent(SummonedIronGolem::onUnSummon);
            entity.removeEffect(ModEffects.GOLEM_TIMER.get());
        }

        SummonedIronGolem summon = new SummonedIronGolem(world, entity);
        summon.setPos(entity.position());
        summon.getAttributes().getInstance(Attributes.ATTACK_DAMAGE).setBaseValue(this.getDamage(spellLevel, entity));
        summon.getAttributes().getInstance(Attributes.MAX_HEALTH).setBaseValue(this.getHealth(spellLevel, entity));
        summon.setHealth(summon.getMaxHealth());
        world.addFreshEntity(summon);
        summon.addEffect(new MobEffectInstance(ModEffects.GOLEM_TIMER.get(), summonTime, 0, false, false, false));
        entity.addEffect(new MobEffectInstance(ModEffects.GOLEM_TIMER.get(), summonTime, 0, false, false, true));

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private float getHealth(int spellLevel, LivingEntity caster) {
        return (float)(20 + spellLevel * 5);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 1 + this.getSpellPower(spellLevel, caster);
    }
}

