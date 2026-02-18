package dev.muon.otherworldorigins.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Tooltip component that displays heart icons (e.g. for goodberry healing).
 */
public record HeartsTooltipComponent(int hearts) implements TooltipComponent {
}
