package dev.muon.otherworldorigins.client.shapeshift;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

/**
 * Maps entity types to their named Citadel attack animations for the shapeshift system.
 * Each animation is keyed by a snake_case string (e.g. "swipe_r", "maul") that can be
 * referenced from the {@code animation} field in shapeshift attack JSON.
 */
@OnlyIn(Dist.CLIENT)
public class ShapeshiftAnimations {

    private static final Map<EntityType<?>, Map<String, Animation>> ATTACK_ANIMATIONS = new IdentityHashMap<>();

    static {
        register(AMEntityRegistry.GRIZZLY_BEAR, Map.of(
                "swipe_r", EntityGrizzlyBear.ANIMATION_SWIPE_R,
                "swipe_l", EntityGrizzlyBear.ANIMATION_SWIPE_L,
                "maul", EntityGrizzlyBear.ANIMATION_MAUL
        ));

        register(AMEntityRegistry.TIGER, Map.of(
                "paw_r", EntityTiger.ANIMATION_PAW_R,
                "paw_l", EntityTiger.ANIMATION_PAW_L
        ));

        register(AMEntityRegistry.SNOW_LEOPARD, Map.of(
                "attack_r", EntitySnowLeopard.ANIMATION_ATTACK_R,
                "attack_l", EntitySnowLeopard.ANIMATION_ATTACK_L
        ));

        register(AMEntityRegistry.CROCODILE, Map.of(
                "lunge", EntityCrocodile.ANIMATION_LUNGE
        ));

        register(AMEntityRegistry.GORILLA, Map.of(
                "attack", EntityGorilla.ANIMATION_ATTACK
        ));

        register(AMEntityRegistry.MOOSE, Map.of(
                "attack", EntityMoose.ANIMATION_ATTACK
        ));

        register(AMEntityRegistry.ELEPHANT, Map.of(
                "stomp", EntityElephant.ANIMATION_STOMP,
                "fling", EntityElephant.ANIMATION_FLING
        ));

        register(AMEntityRegistry.RHINOCEROS, Map.of(
                "fling", EntityRhinoceros.ANIMATION_FLING,
                "slash", EntityRhinoceros.ANIMATION_SLASH
        ));

        register(AMEntityRegistry.BISON, Map.of(
                "attack", EntityBison.ANIMATION_ATTACK
        ));

        register(AMEntityRegistry.DROPBEAR, Map.of(
                "bite", EntityDropBear.ANIMATION_BITE,
                "swipe_r", EntityDropBear.ANIMATION_SWIPE_R,
                "swipe_l", EntityDropBear.ANIMATION_SWIPE_L
        ));

        register(AMEntityRegistry.KANGAROO, Map.of(
                "kick", EntityKangaroo.ANIMATION_KICK,
                "punch_r", EntityKangaroo.ANIMATION_PUNCH_R,
                "punch_l", EntityKangaroo.ANIMATION_PUNCH_L
        ));
    }

    private static void register(net.minecraftforge.registries.RegistryObject<? extends EntityType<?>> typeHolder,
                                 Map<String, Animation> animations) {
        ATTACK_ANIMATIONS.put(typeHolder.get(), animations);
    }

    public static boolean hasAttackAnimations(Entity entity) {
        return entity instanceof IAnimatedEntity
                && ATTACK_ANIMATIONS.containsKey(entity.getType());
    }

    /**
     * Plays the named attack animation on the fake entity. Falls back to the first
     * registered animation if the key is unrecognized, or does nothing if the entity
     * type has no Citadel animations registered.
     */
    public static void triggerAttack(Entity fakeEntity, String animationKey) {
        if (!(fakeEntity instanceof IAnimatedEntity animated)) return;
        Map<String, Animation> anims = ATTACK_ANIMATIONS.get(fakeEntity.getType());
        if (anims == null || anims.isEmpty()) return;

        Animation chosen;
        if (animationKey != null && !animationKey.isEmpty()) {
            chosen = anims.get(animationKey);
        } else {
            chosen = null;
        }
        if (chosen == null) {
            chosen = anims.values().iterator().next();
        }
        animated.setAnimation(chosen);
        animated.setAnimationTick(0);
    }

    public static void tickAnimation(Entity fakeEntity) {
        if (!(fakeEntity instanceof IAnimatedEntity animated)) return;
        Animation current = animated.getAnimation();
        if (current == null || current == IAnimatedEntity.NO_ANIMATION) return;

        int tick = animated.getAnimationTick();
        if (tick < current.getDuration()) {
            animated.setAnimationTick(tick + 1);
        }
        if (animated.getAnimationTick() >= current.getDuration()) {
            animated.setAnimationTick(0);
            animated.setAnimation(IAnimatedEntity.NO_ANIMATION);
        }
    }
}
