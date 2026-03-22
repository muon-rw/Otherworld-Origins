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
 * Maps entity types to their attack animations for the shapeshift system.
 * When a shapeshifted player swings, a random attack animation from the
 * mapped list is played on the fake entity, driving Citadel's ModelAnimator.
 */
@OnlyIn(Dist.CLIENT)
public class ShapeshiftAnimations {

    private static final Map<EntityType<?>, List<Animation>> ATTACK_ANIMATIONS = new IdentityHashMap<>();

    static {
        register(AMEntityRegistry.GRIZZLY_BEAR,
                EntityGrizzlyBear.ANIMATION_SWIPE_R,
                EntityGrizzlyBear.ANIMATION_SWIPE_L,
                EntityGrizzlyBear.ANIMATION_MAUL);

        register(AMEntityRegistry.TIGER,
                EntityTiger.ANIMATION_PAW_R,
                EntityTiger.ANIMATION_PAW_L);

        register(AMEntityRegistry.SNOW_LEOPARD,
                EntitySnowLeopard.ANIMATION_ATTACK_R,
                EntitySnowLeopard.ANIMATION_ATTACK_L);

        register(AMEntityRegistry.CROCODILE,
                EntityCrocodile.ANIMATION_LUNGE);

        register(AMEntityRegistry.GORILLA,
                EntityGorilla.ANIMATION_ATTACK);

        register(AMEntityRegistry.MOOSE,
                EntityMoose.ANIMATION_ATTACK);

        register(AMEntityRegistry.ELEPHANT,
                EntityElephant.ANIMATION_STOMP,
                EntityElephant.ANIMATION_FLING);

        register(AMEntityRegistry.RHINOCEROS,
                EntityRhinoceros.ANIMATION_FLING,
                EntityRhinoceros.ANIMATION_SLASH);

        register(AMEntityRegistry.BISON,
                EntityBison.ANIMATION_ATTACK);

        register(AMEntityRegistry.DROPBEAR,
                EntityDropBear.ANIMATION_BITE,
                EntityDropBear.ANIMATION_SWIPE_R,
                EntityDropBear.ANIMATION_SWIPE_L);

        register(AMEntityRegistry.KANGAROO,
                EntityKangaroo.ANIMATION_KICK,
                EntityKangaroo.ANIMATION_PUNCH_R,
                EntityKangaroo.ANIMATION_PUNCH_L);
    }

    private static void register(net.minecraftforge.registries.RegistryObject<? extends EntityType<?>> typeHolder,
                                 Animation... animations) {
        ATTACK_ANIMATIONS.put(typeHolder.get(), List.of(animations));
    }

    public static boolean hasAttackAnimations(Entity entity) {
        return entity instanceof IAnimatedEntity
                && ATTACK_ANIMATIONS.containsKey(entity.getType());
    }

    public static void triggerRandomAttack(Entity fakeEntity, Random random) {
        if (!(fakeEntity instanceof IAnimatedEntity animated)) return;
        List<Animation> anims = ATTACK_ANIMATIONS.get(fakeEntity.getType());
        if (anims == null || anims.isEmpty()) return;

        Animation chosen = anims.get(random.nextInt(anims.size()));
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
