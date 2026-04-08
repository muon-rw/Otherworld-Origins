package dev.muon.otherworldorigins.action.item;

import dev.muon.otherworldorigins.util.SoulOfArtificeLogic;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.ItemAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.Mutable;

/**
 * Applies one extra Apothic affix chosen from pools allowed by the item's current rarity.
 * Re-running removes the previous bonus affix (if recorded) and rolls a replacement.
 */
public class SoulOfArtificeItemAction extends ItemAction<NoConfiguration> {

    public SoulOfArtificeItemAction() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void execute(NoConfiguration configuration, Level level, Mutable<ItemStack> stackMutable) {
        SoulOfArtificeLogic.applyOnItem(level, stackMutable.getValue());
    }
}
