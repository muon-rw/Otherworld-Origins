package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.network.casting.CastErrorPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class CastSpellAction extends EntityAction<CastSpellAction.Configuration> {
    private static final Map<UUID, ContinuousCastData> CONTINUOUS_CASTS = new HashMap<>();
    private static final double DEFAULT_RAYCAST_DISTANCE = 64.0;

    public CastSpellAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            OtherworldOrigins.LOGGER.debug("Entity is not a LivingEntity: {}", entity);
            return;
        }

        String spellStr = configuration.spell().toString();
        ResourceLocation spellResourceLocation = ResourceLocation.tryParse(spellStr);
        // No one should be using the minecraft namespace anyway, and this is simpler
        if (spellResourceLocation != null && spellResourceLocation.getNamespace().equals("minecraft")) {
            spellResourceLocation = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", spellResourceLocation.getPath());
        }

        AbstractSpell spell = SpellRegistry.getSpell(spellResourceLocation);
        if (spell == null || "none".equals(spell.getSpellName())) {
            OtherworldOrigins.LOGGER.debug("No valid spell found for resource location {}", spellResourceLocation);
            return;
        }

        Level world = entity.level();
        if (world.isClientSide) {
            return;
        }

        int powerLevel = configuration.powerLevel();
        MagicData magicData = MagicData.getPlayerMagicData(livingEntity);

        // Handle case where entity is already casting - varies.
        // TODO: Make this a configurable action json field instead of hardcoded behavior by entity type
        // Requires own impl for cancel, Iron's only has logic for cancelling Player casting
        if (magicData.isCasting()) {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                OtherworldOrigins.LOGGER.debug("CastSpellAction: Player is still casting {}, cancelling previous cast", magicData.getCastingSpellId());
                Utils.serverSideCancelCast(serverPlayer);
            } else {
                OtherworldOrigins.LOGGER.debug("CastSpellAction: Entity is still casting {}, force-completing old cast", magicData.getCastingSpellId());
                AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
                oldSpell.onCast(world, magicData.getCastingSpellLevel(), livingEntity, magicData.getCastSource(), magicData);
                oldSpell.onServerCastComplete(world, magicData.getCastingSpellLevel(), livingEntity, magicData, false);
            }
            magicData.resetCastingState();
            magicData = MagicData.getPlayerMagicData(livingEntity);
        }

        // Raycast to find target and set up targeting data
        LivingEntity target = findTarget(livingEntity, configuration.raycastDistance().orElse(DEFAULT_RAYCAST_DISTANCE));
        if (target != null) {
            updateTargetData(livingEntity, target, magicData, spell, e -> true);
        }

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            castSpellForPlayer(configuration, spell, powerLevel, serverPlayer, magicData, world);
        } else if (livingEntity instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, powerLevel);
        } else {
            // Non-player LivingEntity casting
            if (spell.checkPreCastConditions(world, powerLevel, livingEntity, magicData)) {
                spell.onCast(world, powerLevel, livingEntity, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, powerLevel, livingEntity, magicData, false);
            }
        }
    }

    private void castSpellForPlayer(Configuration configuration, AbstractSpell spell, int powerLevel,
                                    ServerPlayer serverPlayer, MagicData magicData, Level world) {
        Optional<Integer> castTimeOpt = configuration.castTime();
        Optional<Integer> manaCostOpt = configuration.manaCost();

        // Check cast conditions
        CastResult castResult = spell.canBeCastedBy(powerLevel, CastSource.COMMAND, magicData, serverPlayer);
        if (castResult.message != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        if (!castResult.isSuccess() ||
                !spell.checkPreCastConditions(world, powerLevel, serverPlayer, magicData) ||
                MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(serverPlayer, spell.getSpellId(), powerLevel, spell.getSchoolType(), CastSource.COMMAND))) {
            return;
        }

        // Handle mana cost
        if (manaCostOpt.isPresent()) {
            int manaCost = manaCostOpt.get();
            if (!serverPlayer.getAbilities().instabuild && magicData.getMana() < manaCost) {
                PacketDistributor.sendToPlayer(serverPlayer, CastErrorPacket.ErrorType.MANA);
                return;
            }
            if (!serverPlayer.getAbilities().instabuild) {
                setManaWithEvent(serverPlayer, magicData, magicData.getMana() - manaCost);
            }
        }

        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        // Determine effective cast time
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

        // Set up continuous mana drain if configured
        if (configuration.continuousCost() && manaCostOpt.isPresent() && !serverPlayer.getAbilities().instabuild) {
            int manaCost = manaCostOpt.get();
            int costInterval = configuration.costInterval();
            CONTINUOUS_CASTS.put(serverPlayer.getUUID(), new ContinuousCastData(manaCost, costInterval, 0));
        }

        // Initiate the cast
        magicData.initiateCast(spell, powerLevel, effectiveCastTime, CastSource.COMMAND, "command");
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(world, powerLevel, serverPlayer, magicData);

        // Sync casting state to client
        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spell.getSpellId(), powerLevel, effectiveCastTime, CastSource.COMMAND, "command"));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), powerLevel));

        // Log target data if present
        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetingData) {
            LivingEntity target = targetingData.getTarget((ServerLevel) serverPlayer.level());
            if (target != null) {
                OtherworldOrigins.LOGGER.debug("Casting Spell {} with target {}", magicData.getCastingSpellId(), target.getName().getString());
            }
        }

        // For instant cast spells (effectiveCastTime == 0), execute immediately
        if (effectiveCastTime == 0) {
            spell.onCast(world, powerLevel, serverPlayer, CastSource.COMMAND, magicData);
            spell.onServerCastComplete(world, powerLevel, serverPlayer, magicData, false);
            PacketDistributor.sendToPlayer(serverPlayer, new OnClientCastPacket(spell.getSpellId(), powerLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
        }
    }

    /**
     * Raycast from the caster's eyes to find a target entity.
     * Uses a custom block raycast that ignores passable blocks (like grass, flowers)
     * to match the client-side ShoulderSurfing behavior.
     */
    @Nullable
    private LivingEntity findTarget(LivingEntity caster, double distance) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(distance));

        // Use custom block raycast that ignores passable blocks (grass, flowers, etc.)
        HitResult blockHit = clipIgnoringPassableBlocks(caster.level(), eyePos, endPos, caster);

        double searchDist = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(eyePos)
                : distance;

        Vec3 searchEnd = eyePos.add(lookVec.scale(searchDist));

        AABB searchBox = caster.getBoundingBox()
                .expandTowards(lookVec.scale(searchDist))
                .inflate(1.0);

        // Also accept PartEntity instances whose parent is a LivingEntity
        EntityHitResult entityHit = raycastForEntity(caster, eyePos, searchEnd, searchBox,
                e -> !e.isSpectator() && e.isPickable() &&
                        (e instanceof LivingEntity ||
                                (e instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity)),
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

    /**
     * Performs a block raycast that ignores "passable" blocks.
     * A block is considered passable if:
     * - Its collision shape is empty, OR
     * - Its hardness is 0 (instantly breakable, like grass)
     * <p>
     * This matches Better Combat's "swing thru grass" behavior and ensures
     * consistency with the client-side ShoulderSurfing raycast.
     */
    private static HitResult clipIgnoringPassableBlocks(BlockGetter level, Vec3 start, Vec3 end, Entity entity) {
        return BlockGetter.traverseBlocks(start, end, level, (blockGetter, blockPos) -> {
            BlockState blockState = blockGetter.getBlockState(blockPos);

            // Check if this block should be ignored (passable)
            if (isPassableBlock(blockGetter, blockPos, blockState)) {
                return null; // Continue through this block
            }

            // Check for actual collision
            VoxelShape shape = blockState.getCollisionShape(blockGetter, blockPos);
            if (shape.isEmpty()) {
                return null; // No collision shape to hit
            }

            return shape.clip(start, end, blockPos);
        }, (blockGetter) -> {
            // Miss - return end position
            Vec3 direction = start.subtract(end);
            return BlockHitResult.miss(end,
                    Direction.getNearest(direction.x, direction.y, direction.z),
                    BlockPos.containing(end));
        });
    }

    /**
     * Determines if a block should be considered "passable" for raycast purposes.
     * Matches Better Combat's logic for swing-through-grass.
     */
    private static boolean isPassableBlock(BlockGetter level, BlockPos pos, BlockState state) {
        // Empty collision shape = passable (like flowers, small grass, etc.)
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return true;
        }
        // Zero hardness = instantly breakable = passable (like tall grass)
        if (state.getDestroySpeed(level, pos) == 0.0F) {
            return true;
        }
        return false;
    }

    /**
     * Raycast to find an entity within the given bounds.
     */
    @Nullable
    private EntityHitResult raycastForEntity(Entity caster, Vec3 start, Vec3 end, AABB bounds,
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
     * Update the magic data with target information and sync to client.
     */
    public static void updateTargetData(LivingEntity caster, Entity entityHit, MagicData playerMagicData,
                                        AbstractSpell spell, Predicate<LivingEntity> filter) {
        LivingEntity livingTarget = null;

        if (entityHit instanceof LivingEntity livingEntity && filter.test(livingEntity)) {
            livingTarget = livingEntity;
        } else if (entityHit instanceof PartEntity<?> partEntity &&
                partEntity.getParent() instanceof LivingEntity livingParent &&
                filter.test(livingParent)) {
            livingTarget = livingParent;
        }

        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));

            if (caster instanceof ServerPlayer serverPlayer) {
                // if (spell.getCastType() != CastType.INSTANT) {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                // }
//                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
//                        Component.translatable("ui.irons_spellbooks.spell_target_success",
//                                livingTarget.getDisplayName().getString(),
//                                spell.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }

            if (livingTarget instanceof ServerPlayer targetPlayer) {
                Utils.sendTargetedNotification(targetPlayer, caster, spell);
            }
        } else if (caster instanceof ServerPlayer serverPlayer) {
//            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
//                    Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
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
        }
    }

    public static void onSpellEnd(ServerPlayer player) {
        CONTINUOUS_CASTS.remove(player.getUUID());
    }

    /**
     * Register a continuous mana cost for a player. Used by both CastSpellAction and CastSpellBientityAction.
     */
    public static void registerContinuousCast(UUID playerId, int manaCost, int costInterval) {
        CONTINUOUS_CASTS.put(playerId, new ContinuousCastData(manaCost, costInterval, 0));
    }

    private static class ContinuousCastData {
        final int manaCost;
        final int costInterval;
        int ticksElapsed;

        ContinuousCastData(int manaCost, int costInterval, int ticksElapsed) {
            this.manaCost = manaCost;
            this.costInterval = costInterval;
            this.ticksElapsed = ticksElapsed;
        }
    }

    public static class Configuration implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SerializableDataTypes.IDENTIFIER.fieldOf("spell").forGetter(Configuration::spell),
                Codec.INT.optionalFieldOf("power_level", 1).forGetter(Configuration::powerLevel),
                Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
                Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
                Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
                Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
                Codec.DOUBLE.optionalFieldOf("raycast_distance").forGetter(Configuration::raycastDistance)
        ).apply(instance, Configuration::new));

        private final ResourceLocation spell;
        private final int powerLevel;
        private final Optional<Integer> castTime;
        private final Optional<Integer> manaCost;
        private final boolean continuousCost;
        private final int costInterval;
        private final Optional<Double> raycastDistance;

        public Configuration(ResourceLocation spell, int powerLevel, Optional<Integer> castTime,
                             Optional<Integer> manaCost, boolean continuousCost, int costInterval,
                             Optional<Double> raycastDistance) {
            this.spell = spell;
            this.powerLevel = powerLevel;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
            this.raycastDistance = raycastDistance;
        }

        public ResourceLocation spell() {
            return spell;
        }

        public int powerLevel() {
            return powerLevel;
        }

        public Optional<Integer> castTime() {
            return castTime;
        }

        public Optional<Integer> manaCost() {
            return manaCost;
        }

        public boolean continuousCost() {
            return continuousCost;
        }

        public int costInterval() {
            return costInterval;
        }

        public Optional<Double> raycastDistance() {
            return raycastDistance;
        }
    }
}
