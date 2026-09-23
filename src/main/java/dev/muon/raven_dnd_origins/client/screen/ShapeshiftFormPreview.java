package dev.muon.raven_dnd_origins.client.screen;

import dev.muon.raven_dnd_origins.client.shapeshift.FakeEntityCache;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.origins.origin.Origin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public final class ShapeshiftFormPreview {

    public static final int DEFAULT_SIZE = 80;

    private static final int PREVIEW_ENTITY_CACHE_ID = Integer.MIN_VALUE;
    private static final float ISOMETRIC_TILT = (float) Math.toRadians(-20);

    private ShapeshiftFormPreview() {}

    /** Advances client age + walk decay so {@link InventoryScreen#renderEntityInInventory} shows idle posing. */
    public static void tickIdle(Origin origin) {
        LivingEntity living = previewEntity(origin);
        if (living == null) return;

        Player player = Minecraft.getInstance().player;
        double anchorX = player != null ? player.getX() : 0;
        double anchorY = player != null ? player.getY() : 0;
        double anchorZ = player != null ? player.getZ() : 0;

        if (living.tickCount == 0) {
            living.setPos(anchorX, anchorY, anchorZ);
        }

        living.xo = living.getX();
        living.yo = living.getY();
        living.zo = living.getZ();
        living.yBodyRotO = living.yBodyRot;
        living.yRotO = living.getYRot();
        living.yHeadRotO = living.yHeadRot;
        living.xRotO = living.getXRot();

        living.tickCount++;
        living.calculateEntityAnimation(living instanceof FlyingAnimal);
    }

    public static void render(GuiGraphics graphics, Origin origin, int centerX, int centerY, int size, float time) {
        LivingEntity living = previewEntity(origin);
        if (living == null) return;

        float bbHeight = living.getBbHeight();
        float maxDim = Math.max(bbHeight, living.getBbWidth());
        int scale = Math.max(10, (int) (size / maxDim));
        int yPos = centerY + (int) (bbHeight * scale / 2);
        float spinAngleDeg = (float) Math.toDegrees(time * 0.04f);
        renderEntity(graphics, living, centerX, yPos, scale, 180 + spinAngleDeg);
    }

    @Nullable
    private static LivingEntity previewEntity(Origin origin) {
        ResourceLocation entityTypeId = getShapeshiftEntityType(origin);
        if (entityTypeId == null) return null;
        Entity entity = FakeEntityCache.getOrCreate(PREVIEW_ENTITY_CACHE_ID, entityTypeId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void renderEntity(GuiGraphics graphics, LivingEntity living, int x, int y,
                                     int scale, float facingDeg) {
        float savedBodyRot = living.yBodyRot;
        float savedBodyRotO = living.yBodyRotO;
        float savedYRot = living.getYRot();
        float savedXRot = living.getXRot();
        float savedHeadRot = living.yHeadRot;
        float savedHeadRotO = living.yHeadRotO;

        living.yBodyRot = facingDeg;
        living.yBodyRotO = facingDeg;
        living.setYRot(facingDeg);
        living.setXRot(0);
        living.yHeadRot = facingDeg;
        living.yHeadRotO = facingDeg;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(ISOMETRIC_TILT);
        pose.mul(camera);

        ShapeshiftRenderHelper.setRenderingShapeshiftBody(true);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, new Vector3f(), pose, camera, living);
        ShapeshiftRenderHelper.setRenderingShapeshiftBody(false);

        living.yBodyRot = savedBodyRot;
        living.yBodyRotO = savedBodyRotO;
        living.setYRot(savedYRot);
        living.setXRot(savedXRot);
        living.yHeadRot = savedHeadRot;
        living.yHeadRotO = savedHeadRotO;
    }

    @Nullable
    private static ResourceLocation getShapeshiftEntityType(Origin origin) {
        Set<ResourceLocation> visited = new HashSet<>();
        for (ResourceLocation powerId : origin.powers()) {
            ResourceLocation found = findShapeshiftType(powerId, visited);
            if (found != null) return found;
        }
        return null;
    }

    @Nullable
    private static ResourceLocation findShapeshiftType(ResourceLocation powerId, Set<ResourceLocation> visited) {
        if (!visited.add(powerId)) return null;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return null;
        if (power.config() instanceof ShapeshiftPower.Configuration cfg) {
            return cfg.entityType();
        }
        if (power.config() instanceof MultiplePower.Cfg multi) {
            for (ResourceLocation sub : multi.subPowerIds()) {
                ResourceLocation found = findShapeshiftType(sub, visited);
                if (found != null) return found;
            }
        }
        return null;
    }
}
