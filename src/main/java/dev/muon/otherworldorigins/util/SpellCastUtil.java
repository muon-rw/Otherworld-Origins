package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import io.redspace.ironsspellbooks.network.casting.CastErrorPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-side Iron's Spellbooks cast pipeline: COMMAND-source player casts, raycast targeting,
 * continuous mana ticks, and mana events. Used by {@link dev.muon.otherworldorigins.action.entity.CastSpellAction},
 * {@link dev.muon.otherworldorigins.action.bientity.CastSpellBientityAction},
 * {@link dev.muon.otherworldorigins.power.RecastSpellPower}, and mixins.
 */
public final class SpellCastUtil {

    public static final double DEFAULT_RAYCAST_DISTANCE = 64.0;

    private static final Map<UUID, ContinuousCastData> CONTINUOUS_CASTS = new HashMap<>();

    private SpellCastUtil() {
    }

    /**
     * Casts a known spell at {@code powerLevel} with the same pipeline as {@code cast_spell}
     * ({@link CastSource#COMMAND}, optional mana override, etc.). For players only.
     *
     * @return {@code true} if the cast was started (including fully resolved instant casts)
     */
    public static boolean castResolvedSpellForPlayer(
            ServerPlayer serverPlayer,
            AbstractSpell spell,
            int powerLevel,
            Optional<Integer> castTime,
            Optional<Integer> manaCost,
            boolean continuousCost,
            int costInterval,
            Optional<Double> raycastDistance
    ) {
        Level world = serverPlayer.level();
        if (world.isClientSide) {
            return false;
        }
        MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (magicData.isCasting()) {
            boolean sameSpell = spell.getSpellId().equals(magicData.getCastingSpellId());
            OtherworldOrigins.LOGGER.debug("SpellCastUtil.castResolvedSpellForPlayer: cancelling previous cast {}", magicData.getCastingSpellId());
            Utils.serverSideCancelCast(serverPlayer);
            magicData.resetCastingState();
            if (sameSpell) {
                return false;
            }
            magicData = MagicData.getPlayerMagicData(serverPlayer);
        }
        LivingEntity raycastTarget = findTarget(serverPlayer, raycastDistance.orElse(DEFAULT_RAYCAST_DISTANCE));
        return castSpellForPlayer(
                spell,
                powerLevel,
                serverPlayer,
                magicData,
                world,
                raycastTarget,
                castTime,
                manaCost,
                continuousCost,
                costInterval
        );
    }

    /**
     * Runs the COMMAND-source player cast pipeline; {@code raycastTarget} is applied only when the spell
     * already uses {@link TargetEntityCastData} (same as {@code cast_spell}).
     */
    public static boolean castSpellForPlayer(
            AbstractSpell spell,
            int powerLevel,
            ServerPlayer serverPlayer,
            MagicData magicData,
            Level world,
            @Nullable LivingEntity raycastTarget,
            Optional<Integer> castTimeOpt,
            Optional<Integer> manaCostOpt,
            boolean continuousCost,
            int costInterval
    ) {
        return castSpellForPlayerImpl(
                spell,
                powerLevel,
                serverPlayer,
                magicData,
                world,
                raycastTarget,
                castTimeOpt,
                manaCostOpt,
                continuousCost,
                costInterval,
                TargetBindMode.RAYCAST_NULLABLE
        );
    }

    /**
     * Same as {@link #castSpellForPlayer} but binds the actor's bi-entity target into
     * {@link TargetEntityCastData} / {@link TargetedTargetAreaCastData} (no raycast).
     */
    public static boolean castSpellForPlayerWithBientityTarget(
            AbstractSpell spell,
            int powerLevel,
            ServerPlayer serverPlayer,
            MagicData magicData,
            Level world,
            LivingEntity targetFromBientity,
            Optional<Integer> castTimeOpt,
            Optional<Integer> manaCostOpt,
            boolean continuousCost,
            int costInterval
    ) {
        return castSpellForPlayerImpl(
                spell,
                powerLevel,
                serverPlayer,
                magicData,
                world,
                targetFromBientity,
                castTimeOpt,
                manaCostOpt,
                continuousCost,
                costInterval,
                TargetBindMode.BIENTITY_PROVIDED
        );
    }

    private static boolean castSpellForPlayerImpl(
            AbstractSpell spell,
            int powerLevel,
            ServerPlayer serverPlayer,
            MagicData magicData,
            Level world,
            @Nullable LivingEntity targetOrRaycast,
            Optional<Integer> castTimeOpt,
            Optional<Integer> manaCostOpt,
            boolean continuousCost,
            int costInterval,
            TargetBindMode targetBindMode
    ) {
        if (targetBindMode == TargetBindMode.BIENTITY_PROVIDED && targetOrRaycast == null) {
            return false;
        }
        CastResult castResult = spell.canBeCastedBy(powerLevel, CastSource.COMMAND, magicData, serverPlayer);
        if (castResult.message != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        if (!castResult.isSuccess()
                || !spell.checkPreCastConditions(world, powerLevel, serverPlayer, magicData)
                || MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(serverPlayer, spell.getSpellId(), powerLevel, spell.getSchoolType(), CastSource.COMMAND))) {
            return false;
        }

        if (manaCostOpt.isPresent()) {
            int manaCost = manaCostOpt.get();
            if (!serverPlayer.getAbilities().instabuild && magicData.getMana() < manaCost) {
                PacketDistributor.sendToPlayer(serverPlayer, CastErrorPacket.ErrorType.MANA);
                return false;
            }
            if (!serverPlayer.getAbilities().instabuild) {
                setManaWithEvent(serverPlayer, magicData, magicData.getMana() - manaCost);
            }
        }

        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        int effectiveCastTime;
        if (castTimeOpt.isPresent()) {
            effectiveCastTime = castTimeOpt.get();
        } else if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(powerLevel, serverPlayer);
        } else if (spell.getCastType() == CastType.INSTANT) {
            effectiveCastTime = 0;
        } else {
            effectiveCastTime = spell.getEffectiveCastTime(powerLevel, serverPlayer);
        }

        if (continuousCost && manaCostOpt.isPresent() && !serverPlayer.getAbilities().instabuild) {
            int manaCost = manaCostOpt.get();
            CONTINUOUS_CASTS.put(serverPlayer.getUUID(), new ContinuousCastData(manaCost, costInterval, 0));
        }

        magicData.initiateCast(spell, powerLevel, effectiveCastTime, CastSource.COMMAND, "command");
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(world, powerLevel, serverPlayer, magicData);

        if (targetBindMode == TargetBindMode.RAYCAST_NULLABLE) {
            maybeUpdateTargetData(serverPlayer, targetOrRaycast, magicData, spell);
        } else {
            maybeApplyBientityProvidedTarget(serverPlayer, Objects.requireNonNull(targetOrRaycast), magicData, spell);
        }

        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spell.getSpellId(), powerLevel, effectiveCastTime, CastSource.COMMAND, "command"));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), powerLevel));

        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetingData) {
            LivingEntity target = targetingData.getTarget((ServerLevel) serverPlayer.level());
            if (target != null) {
                OtherworldOrigins.LOGGER.debug("Casting Spell {} with target {}", magicData.getCastingSpellId(), target.getName().getString());
            }
        }

        if (effectiveCastTime == 0) {
            spell.onCast(world, powerLevel, serverPlayer, CastSource.COMMAND, magicData);
            PacketDistributor.sendToPlayer(serverPlayer, new OnClientCastPacket(spell.getSpellId(), powerLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
            spell.onServerCastComplete(world, powerLevel, serverPlayer, magicData, false);
        }
        return true;
    }

    /**
     * If the caster was already casting, force-complete that spell (onCast + onServerCastComplete) and reset state.
     * Used by {@link dev.muon.otherworldorigins.action.bientity.CastSpellBientityAction} instead of canceling the prior cast.
     */
    public static void forceCompleteCurrentCastIfAny(LivingEntity caster, Level world) {
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (!magicData.isCasting()) {
            return;
        }
        OtherworldOrigins.LOGGER.debug("SpellCastUtil: force-completing cast {}", magicData.getCastingSpellId());
        AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
        oldSpell.onCast(world, magicData.getCastingSpellLevel(), caster, magicData.getCastSource(), magicData);
        oldSpell.onServerCastComplete(world, magicData.getCastingSpellLevel(), caster, magicData, false);
        magicData.resetCastingState();
    }

    public enum TargetBindMode {
        RAYCAST_NULLABLE,
        BIENTITY_PROVIDED
    }

    /**
     * Raycast from the caster's eyes to find a target entity (passable blocks ignored).
     */
    @Nullable
    public static LivingEntity findTarget(LivingEntity caster, double distance) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(distance));

        HitResult blockHit = clipIgnoringPassableBlocks(caster.level(), eyePos, endPos);

        double searchDist = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(eyePos)
                : distance;

        Vec3 searchEnd = eyePos.add(lookVec.scale(searchDist));

        AABB searchBox = caster.getBoundingBox()
                .expandTowards(lookVec.scale(searchDist))
                .inflate(1.0);

        EntityHitResult entityHit = raycastForEntity(caster, eyePos, searchEnd, searchBox,
                e -> !e.isSpectator() && e.isPickable()
                        && (e instanceof LivingEntity
                        || (e instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity)),
                searchDist);

        if (entityHit != null) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof LivingEntity target) {
                return target;
            } else if (hitEntity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity target) {
                return target;
            }
        }

        return null;
    }

    private static HitResult clipIgnoringPassableBlocks(BlockGetter level, Vec3 start, Vec3 end) {
        return BlockGetter.traverseBlocks(start, end, level, (blockGetter, blockPos) -> {
            BlockState blockState = blockGetter.getBlockState(blockPos);

            if (isPassableBlock(blockGetter, blockPos, blockState)) {
                return null;
            }

            VoxelShape shape = blockState.getCollisionShape(blockGetter, blockPos);
            if (shape.isEmpty()) {
                return null;
            }

            return shape.clip(start, end, blockPos);
        }, (blockGetter) -> {
            Vec3 direction = start.subtract(end);
            return BlockHitResult.miss(end,
                    Direction.getNearest(direction.x, direction.y, direction.z),
                    BlockPos.containing(end));
        });
    }

    private static boolean isPassableBlock(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return true;
        }
        if (state.getDestroySpeed(level, pos) == 0.0F) {
            return true;
        }
        return false;
    }

    @Nullable
    private static EntityHitResult raycastForEntity(Entity caster, Vec3 start, Vec3 end, AABB bounds,
                                                    Predicate<Entity> filter, double maxDistance) {
        double closestDist = maxDistance;
        Entity closestEntity = null;
        Vec3 hitPos = null;

        for (Entity entity : caster.level().getEntities(caster, bounds, filter)) {
            AABB entityBounds = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> optional = entityBounds.clip(start, end);

            if (optional.isPresent()) {
                double dist = start.distanceTo(optional.get());
                if (dist < closestDist) {
                    closestEntity = entity;
                    hitPos = optional.get();
                    closestDist = dist;
                }
            }
        }

        return closestEntity != null ? new EntityHitResult(closestEntity, hitPos) : null;
    }

    /**
     * @param syncTargetingPacket if false, no {@link SyncTargetingDataPacket} — matches ISB
     *        {@link Utils#preCastTargetHelper}, which skips sync for {@link CastType#INSTANT}
     * @param sendTargetSuccessActionBar same helper always sends the green {@code spell_target_success} line when a
     *        target is bound; pass false only for unusual cases
     */
    public static void updateTargetData(LivingEntity caster, Entity entityHit, MagicData playerMagicData,
                                        AbstractSpell spell, Predicate<LivingEntity> filter,
                                        boolean syncTargetingPacket, boolean sendTargetSuccessActionBar) {
        LivingEntity livingTarget = null;

        if (entityHit instanceof LivingEntity livingEntity && filter.test(livingEntity)) {
            livingTarget = livingEntity;
        } else if (entityHit instanceof PartEntity<?> partEntity
                && partEntity.getParent() instanceof LivingEntity livingParent
                && filter.test(livingParent)) {
            livingTarget = livingParent;
        }

        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));

            if (caster instanceof ServerPlayer serverPlayer) {
                if (syncTargetingPacket) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                }
                if (sendTargetSuccessActionBar) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.translatable("ui.irons_spellbooks.spell_target_success",
                                    livingTarget.getDisplayName().getString(),
                                    spell.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
                }
            }

            if (livingTarget instanceof ServerPlayer targetPlayer) {
                Utils.sendTargetedNotification(targetPlayer, caster, spell);
            }
        }
    }

    public static void maybeUpdateTargetData(LivingEntity caster, @Nullable LivingEntity raycastTarget,
                                             MagicData magicData, AbstractSpell spell) {
        ICastData data = magicData.getAdditionalCastData();
        if (raycastTarget != null && data != null && data.getClass() == TargetEntityCastData.class) {
            boolean syncUnlessInstant = spell.getCastType() != CastType.INSTANT;
            updateTargetData(caster, raycastTarget, magicData, spell, e -> true, syncUnlessInstant, true);
        }
    }

    /**
     * After {@code onServerPreCast}, bind the bi-entity target into cast data when the spell uses
     * {@link TargetEntityCastData} or {@link TargetedTargetAreaCastData}.
     * <p>
     * Only {@link TargetEntityCastData} gets client feedback ({@link Utils#preCastTargetHelper} parity). ISB does not
     * auto-notify for targeted-area merge paths — spells handle their own messages.
     * <p>
     * TODO: {@code ImpulseCastData} (e.g. BurningDash) does not support bi-entity targeting — impulse is computed
     * from the caster's look angle inside {@code onCast()} after this runs, so direction cannot be redirected here
     * without patching or wrapping {@code onCast}.
     */
    public static void maybeApplyBientityProvidedTarget(LivingEntity caster, LivingEntity providedTarget,
                                                         MagicData magicData, AbstractSpell spell) {
        ICastData data = magicData.getAdditionalCastData();
        if (data != null && data.getClass() == TargetEntityCastData.class) {
            boolean syncUnlessInstant = spell.getCastType() != CastType.INSTANT;
            updateTargetData(caster, providedTarget, magicData, spell, e -> true, syncUnlessInstant, true);
        } else if (data instanceof TargetedTargetAreaCastData targetedArea) {
            TargetedAreaEntity areaEntity = targetedArea.getAreaEntity();
            areaEntity.setOwner(providedTarget);
            areaEntity.setPos(providedTarget.position());
            magicData.setAdditionalCastData(new TargetedTargetAreaCastData(providedTarget, areaEntity));
        }
    }

    public static void onSpellTick(ServerPlayer player, MagicData magicData) {
        UUID playerId = player.getUUID();
        ContinuousCastData data = CONTINUOUS_CASTS.get(playerId);
        if (data != null) {
            data.ticksElapsed++;
            if (data.ticksElapsed >= data.costInterval) {
                data.ticksElapsed = 0;
                if (magicData.getMana() >= data.manaCost) {
                    setManaWithEvent(player, magicData, magicData.getMana() - data.manaCost);
                    OtherworldOrigins.LOGGER.debug("Draining mana: {}. Remaining mana: {}", data.manaCost, magicData.getMana());
                } else {
                    Utils.serverSideCancelCast(player);
                    CONTINUOUS_CASTS.remove(playerId);
                }
            }
        }
    }

    private static void setManaWithEvent(ServerPlayer player, MagicData magicData, float newMana) {
        ChangeManaEvent event = new ChangeManaEvent(player, magicData, magicData.getMana(), newMana);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            magicData.setMana(event.getNewMana());
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }

    /**
     * Helper to drain a fixed amount of mana from a player. 
     */
    public static void drainPlayerMana(ServerPlayer player, float amount) {
        if (amount <= 0 || player.getAbilities().instabuild) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(player);
        setManaWithEvent(player, magicData, Math.max(0f, magicData.getMana() - amount));
    }

    public static void onSpellEnd(ServerPlayer player) {
        CONTINUOUS_CASTS.remove(player.getUUID());
    }

    /**
     * Registers continuous mana drain without going through {@link #castSpellForPlayer}; same backing map as
     * {@code continuous_cost} in the cast pipeline.
     */
    public static void registerContinuousCast(UUID playerId, int manaCost, int costInterval) {
        CONTINUOUS_CASTS.put(playerId, new ContinuousCastData(manaCost, costInterval, 0));
    }

    private static final class ContinuousCastData {
        final int manaCost;
        final int costInterval;
        int ticksElapsed;

        ContinuousCastData(int manaCost, int costInterval, int ticksElapsed) {
            this.manaCost = manaCost;
            this.costInterval = costInterval;
            this.ticksElapsed = ticksElapsed;
        }
    }
}
