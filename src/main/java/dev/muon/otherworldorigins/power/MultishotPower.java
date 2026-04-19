package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * When an {@link AbstractArrow} is fired by the owner, spawn {@code amount} additional copies on each side,
 * rotated by multiples of {@code angle} degrees around the shooter's up vector.
 * Extras are NBT-cloned so pierce level, base damage, potion effects, etc. carry over.
 * Clones set delta movement directly to avoid re-triggering shoot() hooks (no recursion).
 */
public class MultishotPower extends PowerFactory<MultishotPower.Configuration> {

    public MultishotPower() {
        super(Configuration.CODEC);
    }

    public static void spawnExtras(AbstractArrow source, Entity owner) {
        if (source.level().isClientSide() || source.isRemoved()) {
            return;
        }
        IPowerContainer.get(owner).ifPresent(container -> {
            for (Holder<ConfiguredPower<Configuration, MultishotPower>> holder :
                    container.getPowers(ModPowers.MULTISHOT.get())) {
                if (!holder.isBound() || !holder.value().isActive(owner)) {
                    continue;
                }
                Configuration cfg = holder.value().getConfiguration();
                spawn(source, owner, cfg);
                ConfiguredEntityAction.execute(cfg.selfAction(), owner);
            }
        });
    }

    private static void spawn(AbstractArrow source, Entity owner, Configuration cfg) {
        int amount = Math.max(0, cfg.amount());
        if (amount == 0) {
            return;
        }
        Level level = source.level();
        Vec3 delta = source.getDeltaMovement();
        Vec3 up = owner.getUpVector(1.0F);

        for (int side = 1; side <= amount; side++) {
            float positive = side * cfg.angle();
            float negative = -positive;
            AbstractArrow right = cloneArrow(source, owner, level, delta, up, positive);
            if (right != null) {
                level.addFreshEntity(right);
            }
            AbstractArrow left = cloneArrow(source, owner, level, delta, up, negative);
            if (left != null) {
                level.addFreshEntity(left);
            }
        }
    }

    private static AbstractArrow cloneArrow(AbstractArrow source, Entity owner, Level level, Vec3 delta, Vec3 up, float degrees) {
        Entity created = source.getType().create(level);
        if (!(created instanceof AbstractArrow copy)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        CompoundTag tag = new CompoundTag();
        source.saveWithoutId(tag);
        copy.load(tag);
        copy.setUUID(UUID.randomUUID());
        copy.setPos(source.getX(), source.getY(), source.getZ());
        copy.setOwner(owner);
        copy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        Quaternionf rotation = new Quaternionf().setAngleAxis(degrees * ((float) Math.PI / 180F), (float) up.x, (float) up.y, (float) up.z);
        Vector3f rotated = delta.toVector3f().rotate(rotation);
        Vec3 rotatedVec = new Vec3(rotated.x(), rotated.y(), rotated.z());
        copy.setDeltaMovement(rotatedVec);

        double horizontal = rotatedVec.horizontalDistance();
        copy.setYRot((float) (Math.toDegrees(Math.atan2(rotatedVec.x, rotatedVec.z))));
        copy.setXRot((float) (Math.toDegrees(Math.atan2(rotatedVec.y, horizontal))));
        return copy;
    }

    public record Configuration(int amount, float angle, Holder<ConfiguredEntityAction<?, ?>> selfAction) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Configuration::amount),
                Codec.FLOAT.optionalFieldOf("angle", 10.0F).forGetter(Configuration::angle),
                ConfiguredEntityAction.optional("self_action").forGetter(Configuration::selfAction)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return amount >= 0;
        }
    }
}
