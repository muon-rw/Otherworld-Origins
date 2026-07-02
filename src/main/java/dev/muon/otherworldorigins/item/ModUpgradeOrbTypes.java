package dev.muon.otherworldorigins.item;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import net.minecraft.resources.ResourceKey;

/**
 * Keys into Iron's Spellbooks' upgrade_orb_type datapack registry.
 * Entries are defined in data/otherworldorigins/irons_spellbooks/upgrade_orb_type/.
 */
public class ModUpgradeOrbTypes {
    public static final ResourceKey<UpgradeOrbType> MELEE_DAMAGE =
            ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY, OtherworldOrigins.loc("melee_damage"));
    public static final ResourceKey<UpgradeOrbType> ARROW_DAMAGE =
            ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY, OtherworldOrigins.loc("arrow_damage"));
}
