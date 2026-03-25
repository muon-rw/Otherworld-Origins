package dev.muon.otherworldorigins.client.shapeshift;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Drives vanilla {@link AnimationState}-based animations on shapeshifted fake entities.
 * Works with any entity that exposes public AnimationState fields (e.g. Legendary Monsters,
 * vanilla keyframe-animated mobs).
 * <p>
 * Fields are classified by name into action categories and driven by the source player's
 * state each frame. Reflection runs once per entity class; per-player {@link AnimationState}
 * instances are cached so the render hot path involves zero reflective access.
 * <p>
 * Priority (highest first): attack, land, fly, fall, block, run, sit, idle.
 */
@OnlyIn(Dist.CLIENT)
public class VanillaAnimationSync {

    private static final int ATTACK_ANIM_TICKS = 20;
    private static final int LAND_ANIM_TICKS = 8;

    private static final Set<String> ATTACK_KEYWORDS = Set.of(
            "attack", "combo", "slash", "stab", "slam", "smash", "bite",
            "slap", "uppercut", "upper", "punch", "kick", "fling", "stomp",
            "shoot", "spin", "tongue", "toungue", "lunge", "swipe"
    );

    private enum AnimCategory { IDLE, ATTACK, RUN, SIT, BLOCK, FALL, LAND, FLY, SWIM, SKIP }

    private record TypeConfig(
            @Nullable Field idleField,
            List<Field> attackFields,
            @Nullable Field runField,
            @Nullable Field sitField,
            @Nullable Field blockField,
            @Nullable Field fallField,
            @Nullable Field landField,
            @Nullable Field flyField,
            @Nullable Field swimField
    ) {}

    private record PlayerCache(
            Entity entity,
            @Nullable AnimationState idle,
            List<AnimationState> attacks,
            Map<String, AnimationState> namedAttacks,
            @Nullable AnimationState run,
            @Nullable AnimationState sit,
            @Nullable AnimationState block,
            @Nullable AnimationState fall,
            @Nullable AnimationState land,
            @Nullable AnimationState fly,
            @Nullable AnimationState swim,
            List<AnimationState> allStates
    ) {}

    private static final Map<Class<?>, Optional<TypeConfig>> TYPE_CONFIGS = new IdentityHashMap<>();
    private static final Map<Integer, PlayerCache> PLAYER_CACHES = new HashMap<>();
    private static final Map<Integer, Integer> ATTACK_END_TICK = new HashMap<>();
    private static final Map<Integer, Boolean> PREV_ON_GROUND = new HashMap<>();
    private static final Map<Integer, Integer> LAND_END_TICK = new HashMap<>();

    // ---- Public API ----

    /**
     * Triggers a named attack animation on the fake entity. The key is matched against
     * the normalized field name (e.g. {@code upperCutAnimationState} → {@code "upper_cut"}).
     * Falls back to the first registered attack animation if the key is unrecognized.
     *
     * @return true if an attack animation was started
     */
    public static boolean triggerNamedAttack(int entityId, Entity target, String animationKey) {
        PlayerCache cache = getOrBuildCache(entityId, target);
        if (cache == null || cache.attacks.isEmpty()) return false;

        int tick = target.tickCount;
        AnimationState chosen = null;
        if (animationKey != null && !animationKey.isEmpty()) {
            chosen = cache.namedAttacks.get(animationKey);
        }
        if (chosen == null) {
            chosen = cache.attacks.get(0);
        }

        cache.allStates.forEach(AnimationState::stop);
        chosen.start(tick);
        ATTACK_END_TICK.put(entityId, tick + ATTACK_ANIM_TICKS);
        LAND_END_TICK.remove(entityId);
        return true;
    }

    /**
     * Syncs vanilla AnimationState animations on the fake entity based on the source
     * player's state. Called every frame from {@link ShapeshiftRenderHelper#syncVisualState}.
     * <p>
     * Attack animations are no longer triggered here — they are driven externally via
     * {@link #triggerNamedAttack} from {@link ShapeshiftRenderHelper}.
     */
    public static void syncAnimations(Entity source, Entity target) {
        PlayerCache cache = getOrBuildCache(source.getId(), target);
        if (cache == null) return;

        int id = source.getId();
        int tick = target.tickCount;
        LivingEntity livingSource = source instanceof LivingEntity le ? le : null;

        boolean inAttack = isTimerActive(ATTACK_END_TICK, id, tick);
        boolean inLand = isTimerActive(LAND_END_TICK, id, tick);

        if (cache.land != null && !inAttack) {
            boolean wasOnGround = PREV_ON_GROUND.getOrDefault(id, true);
            boolean nowOnGround = source.onGround();
            PREV_ON_GROUND.put(id, nowOnGround);

            if (nowOnGround && !wasOnGround) {
                cache.allStates.forEach(AnimationState::stop);
                cache.land.start(tick);
                LAND_END_TICK.put(id, tick + LAND_ANIM_TICKS);
                inLand = true;
            }
        }

        expireTimer(ATTACK_END_TICK, id, tick, cache.attacks);
        if (cache.land != null) expireTimer(LAND_END_TICK, id, tick, List.of(cache.land));

        boolean timed = inAttack || inLand;
        boolean swimming = !timed && cache.swim  != null && source.isSwimming();
        boolean flying   = !timed && !swimming && cache.fly   != null && livingSource != null && livingSource.isFallFlying();
        boolean falling  = !timed && !swimming && !flying && cache.fall  != null && !source.onGround();
        boolean blocking = !timed && !swimming && !flying && !falling && cache.block != null && livingSource != null && livingSource.isBlocking();
        boolean sprinting= !timed && !swimming && !flying && !falling && !blocking && cache.run   != null && source.isSprinting();
        boolean sneaking = !timed && !swimming && !flying && !falling && !blocking && !sprinting && cache.sit   != null && source.isCrouching();
        boolean idle     = !timed && !swimming && !flying && !falling && !blocking && !sprinting && !sneaking;

        if (cache.swim  != null) cache.swim.animateWhen(swimming, tick);
        if (cache.fly   != null) cache.fly.animateWhen(flying, tick);
        if (cache.fall  != null) cache.fall.animateWhen(falling, tick);
        if (cache.block != null) cache.block.animateWhen(blocking, tick);
        if (cache.run   != null) cache.run.animateWhen(sprinting, tick);
        if (cache.sit   != null) cache.sit.animateWhen(sneaking, tick);
        if (cache.idle  != null) cache.idle.animateWhen(idle, tick);
    }

    public static void evict(int playerId) {
        PLAYER_CACHES.remove(playerId);
        ATTACK_END_TICK.remove(playerId);
        PREV_ON_GROUND.remove(playerId);
        LAND_END_TICK.remove(playerId);
    }

    public static void clearAll() {
        PLAYER_CACHES.clear();
        ATTACK_END_TICK.clear();
        PREV_ON_GROUND.clear();
        LAND_END_TICK.clear();
        TYPE_CONFIGS.clear();
    }

    // ---- Timer helpers ----

    private static boolean isTimerActive(Map<Integer, Integer> timers, int entityId, int tick) {
        Integer end = timers.get(entityId);
        return end != null && tick < end;
    }

    private static void expireTimer(Map<Integer, Integer> timers, int entityId, int tick,
                                    List<AnimationState> toStop) {
        Integer end = timers.get(entityId);
        if (end != null && tick >= end) {
            toStop.forEach(AnimationState::stop);
            timers.remove(entityId);
        }
    }

    // ---- Cache building (one-time reflection per entity class) ----

    @Nullable
    private static PlayerCache getOrBuildCache(int playerId, Entity entity) {
        PlayerCache existing = PLAYER_CACHES.get(playerId);
        if (existing != null && existing.entity == entity) return existing;

        TypeConfig tc = getOrBuildTypeConfig(entity.getClass());
        if (tc == null) return null;

        try {
            AnimationState idle  = readField(tc.idleField, entity);
            AnimationState run   = readField(tc.runField, entity);
            AnimationState sit   = readField(tc.sitField, entity);
            AnimationState block = readField(tc.blockField, entity);
            AnimationState fall  = readField(tc.fallField, entity);
            AnimationState land  = readField(tc.landField, entity);
            AnimationState fly   = readField(tc.flyField, entity);
            AnimationState swim  = readField(tc.swimField, entity);

            List<AnimationState> attacks = new ArrayList<>();
            Map<String, AnimationState> namedAttacks = new LinkedHashMap<>();
            for (Field f : tc.attackFields) {
                AnimationState s = readField(f, entity);
                if (s != null) {
                    attacks.add(s);
                    namedAttacks.put(normalizeFieldName(f.getName()), s);
                }
            }

            List<AnimationState> all = new ArrayList<>();
            if (idle != null) all.add(idle);
            all.addAll(attacks);
            for (AnimationState s : new AnimationState[]{run, sit, block, fall, land, fly, swim}) {
                if (s != null) all.add(s);
            }
            if (all.isEmpty()) return null;

            PlayerCache cache = new PlayerCache(
                    entity, idle, List.copyOf(attacks), Map.copyOf(namedAttacks),
                    run, sit, block, fall, land, fly, swim, List.copyOf(all));
            PLAYER_CACHES.put(playerId, cache);
            return cache;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Nullable
    private static AnimationState readField(@Nullable Field field, Entity entity)
            throws IllegalAccessException {
        return field != null ? (AnimationState) field.get(entity) : null;
    }

    @Nullable
    private static TypeConfig getOrBuildTypeConfig(Class<?> clazz) {
        return TYPE_CONFIGS.computeIfAbsent(clazz, c -> {
            Field idleField = null, runField = null, sitField = null, blockField = null;
            Field fallField = null, landField = null, flyField = null, swimField = null;
            List<Field> attackFields = new ArrayList<>();

            for (Field field : c.getFields()) {
                if (field.getType() != AnimationState.class) continue;
                switch (classify(field.getName())) {
                    case IDLE    -> idleField = field;
                    case ATTACK  -> attackFields.add(field);
                    case RUN     -> runField = field;
                    case SIT     -> sitField = field;
                    case BLOCK   -> blockField = field;
                    case FALL    -> fallField = field;
                    case LAND    -> landField = field;
                    case FLY     -> flyField = field;
                    case SWIM    -> swimField = field;
                    case SKIP    -> {}
                }
            }

            boolean hasAnything = idleField != null || !attackFields.isEmpty()
                    || runField != null || sitField != null || blockField != null
                    || fallField != null || landField != null || flyField != null
                    || swimField != null;

            if (!hasAnything) return Optional.empty();

            return Optional.of(new TypeConfig(
                    idleField, List.copyOf(attackFields),
                    runField, sitField, blockField, fallField, landField, flyField, swimField));
        }).orElse(null);
    }

    // ---- Field name classification ----

    private static AnimCategory classify(String fieldName) {
        String name = fieldName.toLowerCase(Locale.ROOT);

        if (name.contains("death") || name.contains("die")) return AnimCategory.SKIP;
        if (name.contains("idle")) return AnimCategory.IDLE;

        if ((name.contains("run") || name.contains("sprint"))
                && !name.contains("pre") && !name.contains("post")) {
            return AnimCategory.RUN;
        }
        if (name.contains("sit")
                && !name.contains("start") && !name.contains("end")) {
            return AnimCategory.SIT;
        }
        if ((name.contains("block") || name.contains("shield"))
                && !name.contains("hit") && !name.contains("stun")) {
            return AnimCategory.BLOCK;
        }
        if (name.contains("fall") && !name.contains("fracture")) return AnimCategory.FALL;
        if (name.contains("land") && !name.contains("fracture")) return AnimCategory.LAND;
        if (name.contains("fly") || name.contains("glide")) return AnimCategory.FLY;
        if (name.contains("swim")) return AnimCategory.SWIM;

        for (String keyword : ATTACK_KEYWORDS) {
            if (name.contains(keyword)) return AnimCategory.ATTACK;
        }

        return AnimCategory.SKIP;
    }

    /**
     * Converts an AnimationState field name to a snake_case key for JSON matching.
     * {@code upperCutAnimationState} → {@code "upper_cut"},
     * {@code slapAnimationState} → {@code "slap"}.
     */
    private static String normalizeFieldName(String fieldName) {
        String name = fieldName;
        if (name.endsWith("AnimationState")) {
            name = name.substring(0, name.length() - "AnimationState".length());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
