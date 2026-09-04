package dev.muon.raven_dnd_origins.restrictions;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.power.AllowedSpellsPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellRestrictions {

    public static Component getRestrictionMessage(Player player, AbstractSpell spell) {
        return Component.translatable("raven_dnd_origins.restriction.not_attuned")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static final long UNRESOLVED_WARN_INTERVAL_MS = 10_000L;
    private static final Map<UUID, Long> lastUnresolvedWarn = new ConcurrentHashMap<>();

    /**
     * Tooltip variant: shows a restriction only once the container is known. The client asks this
     * for every scroll and spellbook while JEI builds its ingredient filter at login, before the
     * container sync has arrived, so an unresolved container is expected there and not logged.
     */
    public static boolean isSpellRestrictedForDisplay(Player player, AbstractSpell spell) {
        return PowerContainer.of(player) != null && !isSpellAllowed(player, spell);
    }

    public static boolean isSpellAllowed(Player player, AbstractSpell spell) {
        if (!RavenDndOriginsConfig.enableSpellRestrictions()) {
            return true;
        }
        if (isUnrestricted(spell)) {
            return true;
        }

        if (PowerContainer.of(player) == null) {
            // Fail open on a transient container miss (e.g. a desynced sync window around
            // dimension change / respawn / login) so the player isn't hard-blocked from every
            // spell. A persistent null on the server is still a bug worth surfacing in logs.
            warnUnresolved(player, spell);
            return true;
        }

        for (AllowedSpellsPower.Configuration config
                : PowerLookup.active(player, ModPowers.ALLOWED_SPELLS, AllowedSpellsPower.Configuration.class)) {
            if (config.isSpellAllowed(spell)) {
                return true;
            }
        }
        return false;
    }

    private static void warnUnresolved(Player player, AbstractSpell spell) {
        if (player.level().isClientSide()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastUnresolvedWarn.get(player.getUUID());
        if (last != null && now - last < UNRESOLVED_WARN_INTERVAL_MS) {
            return;
        }
        lastUnresolvedWarn.put(player.getUUID(), now);
        RavenDndOrigins.LOGGER.warn(
                "Power container unresolved for player {} (uuid={}, dim={}) when checking spell '{}'; allowing cast",
                player.getName().getString(),
                player.getUUID(),
                player.level().dimension().location(),
                spell.getSpellId());
    }

    private static boolean isUnrestricted(AbstractSpell spell) {
        return SpellRegistry.REGISTRY.getTag(ModSpellTags.UNRESTRICTED)
                .map(tag -> tag.stream().map(Holder::value).anyMatch(spell::equals))
                .orElse(false);
    }
}
