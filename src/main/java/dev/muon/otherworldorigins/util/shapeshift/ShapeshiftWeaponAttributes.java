package dev.muon.otherworldorigins.util.shapeshift;

import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and caches Better Combat {@link WeaponAttributes} from shapeshift power
 * configurations, and resolves the current {@link AttackHand} for combo cycling.
 */
public class ShapeshiftWeaponAttributes {

    private static final Map<ResourceLocation, WeaponAttributes> CACHE = new ConcurrentHashMap<>();

    private static final WeaponAttributes.Attack[] DEFAULT_ATTACKS = new WeaponAttributes.Attack[]{
            new WeaponAttributes.Attack(
                    null, WeaponAttributes.HitBoxShape.HORIZONTAL_PLANE,
                    1.0, 100, 0.5,
                    "bettercombat:one_handed_slash_horizontal_right",
                    null, null
            ),
            new WeaponAttributes.Attack(
                    null, WeaponAttributes.HitBoxShape.FORWARD_BOX,
                    1.1, 50, 0.5,
                    "bettercombat:one_handed_stab",
                    null, null
            )
    };

    /** {@code AttackHand} must carry the real main-hand stack or BC resets combo when item != air. */
    @Nullable
    public static AttackHand resolve(Player player, ShapeshiftPower.Configuration config, int comboCount) {
        WeaponAttributes attrs = getOrBuild(config);
        if (attrs == null || attrs.attacks() == null || attrs.attacks().length == 0) return null;

        int attackIndex = Math.floorMod(comboCount, attrs.attacks().length);
        WeaponAttributes.Attack attack = attrs.attacks()[attackIndex];
        ComboState combo = new ComboState(attackIndex + 1, attrs.attacks().length);
        ItemStack held = player != null ? player.getMainHandItem().copy() : ItemStack.EMPTY;
        return new AttackHand(attack, combo, false, attrs, held);
    }

    @Nullable
    public static WeaponAttributes getOrBuild(ShapeshiftPower.Configuration config) {
        try {
            return CACHE.computeIfAbsent(config.entityType(), id -> build(config));
        } catch (Exception e) {
            return CACHE.get(config.entityType());
        }
    }

    private static WeaponAttributes build(ShapeshiftPower.Configuration config) {
        double range = config.attackRange();
        if (range <= 0) {
            range = deriveRange(config.entityType());
        }

        WeaponAttributes.Attack[] attacks;
        if (config.attacks().isEmpty()) {
            attacks = DEFAULT_ATTACKS;
        } else {
            attacks = convertAttacks(config.attacks());
        }

        return new WeaponAttributes(range, null, null, false, "beast", attacks);
    }

    private static double deriveRange(ResourceLocation entityTypeId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (type == null) return 2.5;
        float width = type.getDimensions().width;
        return Math.max(width, 1.5) + 0.5;
    }

    private static WeaponAttributes.Attack[] convertAttacks(List<ShapeshiftPower.ShapeshiftAttack> list) {
        WeaponAttributes.Attack[] result = new WeaponAttributes.Attack[list.size()];
        int horizontalAlternator = 0;

        for (int i = 0; i < list.size(); i++) {
            ShapeshiftPower.ShapeshiftAttack sa = list.get(i);
            WeaponAttributes.HitBoxShape shape = parseHitbox(sa.hitbox());
            String bcAnim = pickBcAnimation(shape, horizontalAlternator);
            if (shape == WeaponAttributes.HitBoxShape.HORIZONTAL_PLANE) {
                horizontalAlternator++;
            }
            result[i] = new WeaponAttributes.Attack(
                    null, shape,
                    sa.damageMultiplier(), sa.angle(), sa.upswing(),
                    bcAnim,
                    null, null
            );
        }
        return result;
    }

    private static String pickBcAnimation(WeaponAttributes.HitBoxShape shape, int alternator) {
        return switch (shape) {
            case HORIZONTAL_PLANE -> alternator % 2 == 0
                    ? "bettercombat:one_handed_slash_horizontal_right"
                    : "bettercombat:one_handed_slash_horizontal_left";
            case VERTICAL_PLANE -> "bettercombat:one_handed_slam";
            case FORWARD_BOX -> "bettercombat:one_handed_stab";
        };
    }

    private static WeaponAttributes.HitBoxShape parseHitbox(String name) {
        return switch (name.toUpperCase()) {
            case "FORWARD_BOX" -> WeaponAttributes.HitBoxShape.FORWARD_BOX;
            case "VERTICAL_PLANE" -> WeaponAttributes.HitBoxShape.VERTICAL_PLANE;
            default -> WeaponAttributes.HitBoxShape.HORIZONTAL_PLANE;
        };
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
