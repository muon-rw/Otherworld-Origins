package dev.muon.otherworldorigins.util.shapeshift;

/**
 * Hitbox override while shapeshifted (width/height in blocks, same units as {@link net.minecraft.world.entity.EntityDimensions}).
 */
public record ShapeshiftCollisionShape(float width, float height) {}
