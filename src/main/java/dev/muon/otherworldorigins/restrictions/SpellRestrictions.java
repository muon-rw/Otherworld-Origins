package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.power.AllowedSpellsPower;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.List;

public class SpellRestrictions {

    public static Component getRestrictionMessage(Player player, AbstractSpell spell) {
        return Component.translatable("otherworldorigins.restriction.not_attuned")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static boolean isSpellAllowed(Player player, AbstractSpell spell) {
        ITagManager<AbstractSpell> tagManager = SpellRegistry.REGISTRY.get().tags();
        if (tagManager != null && tagManager.getTag(ModSpellTags.UNRESTRICTED).contains(spell)) {
            return true;
        }

        var container = IPowerContainer.get(player).resolve().orElse(null);
        if (container == null) return false;

        List<Holder<ConfiguredPower<AllowedSpellsPower.Configuration, ?>>> powers =
                (List) container.getPowers(ModPowers.ALLOWED_SPELLS.get());

        for (var holder : powers) {
            AllowedSpellsPower.Configuration config = holder.value().getConfiguration();
            if (config.isSpellAllowed(spell)) {
                return true;
            }
        }
        return false;
    }
}
