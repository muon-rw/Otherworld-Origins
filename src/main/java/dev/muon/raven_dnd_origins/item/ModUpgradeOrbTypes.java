package dev.muon.raven_dnd_origins.item;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import net.minecraft.resources.ResourceKey;

/**
 * Keys into Iron's Spellbooks' upgrade_orb_type datapack registry.
 * Entries are defined in data/raven_dnd_origins/irons_spellbooks/upgrade_orb_type/.
 */
public class ModUpgradeOrbTypes {
    public static final ResourceKey<UpgradeOrbType> MELEE_DAMAGE =
            ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY, RavenDndOrigins.loc("melee_damage"));
    public static final ResourceKey<UpgradeOrbType> ARROW_DAMAGE =
            ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY, RavenDndOrigins.loc("arrow_damage"));
}
