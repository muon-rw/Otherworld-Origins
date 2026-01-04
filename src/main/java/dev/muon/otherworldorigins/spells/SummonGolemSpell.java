package dev.muon.otherworldorigins.spells;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.entity.summons.SummonedIronGolem;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

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

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

    @Override
    public boolean allowLooting() {
        return false;
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

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        if (SummonManager.recastFinishedHelper(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new SummonedEntitiesCastData();
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int summonTime = 20 * 60 * 10;

        PlayerRecasts recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)) {
            SummonedEntitiesCastData summonedEntitiesCastData = new SummonedEntitiesCastData();

            SummonedIronGolem golem = new SummonedIronGolem(world, entity);
            golem.setPos(entity.position());

            golem.getAttributes().getInstance(Attributes.ATTACK_DAMAGE).setBaseValue(getDamage(spellLevel, entity));
            golem.getAttributes().getInstance(Attributes.MAX_HEALTH).setBaseValue(getHealth(spellLevel, entity));
            golem.setHealth(golem.getMaxHealth());
            world.addFreshEntity(golem);
            SummonManager.initSummon(entity, golem, summonTime, summonedEntitiesCastData);

            RecastInstance recastInstance = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), summonTime, castSource, summonedEntitiesCastData);
            recasts.addRecast(recastInstance, playerMagicData);
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }

    private float getHealth(int spellLevel, LivingEntity caster) {
        return (float)(20 + spellLevel * 5);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return 1 + this.getSpellPower(spellLevel, caster);
    }
}

