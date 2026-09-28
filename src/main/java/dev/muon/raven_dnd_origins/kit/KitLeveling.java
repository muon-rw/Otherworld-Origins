package dev.muon.raven_dnd_origins.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.client.SyncAptitudeCapabilityCP;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistryPassives;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import com.seniors.justlevelingfork.registry.passive.Passive;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aptitudes are stored as base levels: innate bonuses from origins are added on top, so a kit applies the same way
 * whichever origins it grants.
 */
final class KitLeveling {
    private static final String ALL = "all";
    private static final String MAX = "max";

    private KitLeveling() {}

    // Steps passives down one call at a time rather than resetting the map, so passive-change listeners stay in sync
    static void reset(AptitudeCapability capability) {
        for (Passive passive : RegistryPassives.PASSIVES_REGISTRY.getValues()) {
            int level = capability.getPassiveLevel(passive);
            if (level > 0) {
                capability.subPassiveLevel(passive, level);
            }
        }
        for (Skill skill : RegistrySkills.SKILLS_REGISTRY.getValues()) {
            if (capability.getToggleSkill(skill)) {
                toggle(capability, skill, false);
            }
        }
    }

    static void setAptitudes(ServerPlayer player, AptitudeCapability capability, JsonObject aptitudes, List<String> problems) {
        Set<String> known = new HashSet<>();
        for (Aptitude aptitude : RegistryAptitudes.APTITUDES_REGISTRY.getValues()) {
            String name = aptitude.getName();
            known.add(name);
            int base = aptitudes.has(name) ? Math.max(1, aptitudes.get(name).getAsInt()) : 1;
            capability.setAptitudeLevel(aptitude, base + InnateAptitudeBonusPower.getBonus(player, name));
        }
        aptitudes.keySet().stream().filter(name -> !known.contains(name)).forEach(name -> problems.add("unknown aptitude " + name));
    }

    static void setPassives(AptitudeCapability capability, JsonElement spec, List<String> problems) {
        boolean max = isKeyword(spec, MAX);
        JsonObject levels = max ? new JsonObject() : spec.getAsJsonObject();
        Set<String> known = new HashSet<>();
        for (Passive passive : RegistryPassives.PASSIVES_REGISTRY.getValues()) {
            known.add(passive.getName());
            if (!RegistryPassives.isEnabled(passive)) {
                continue;
            }
            int allowed = allowedLevel(capability, passive);
            int wanted = max ? allowed : levels.has(passive.getName()) ? levels.get(passive.getName()).getAsInt() : 0;
            if (wanted > allowed) {
                problems.add("passive " + passive.getName() + " capped at " + allowed + " by its aptitude");
                wanted = allowed;
            }
            int current = capability.getPassiveLevel(passive);
            if (wanted > current) {
                capability.addPassiveLevel(passive, wanted - current);
            } else if (wanted < current) {
                capability.subPassiveLevel(passive, current - wanted);
            }
        }
        levels.keySet().stream().filter(name -> !known.contains(name)).forEach(name -> problems.add("unknown passive " + name));
    }

    static void setSkills(ServerPlayer player, AptitudeCapability capability, JsonElement spec, List<String> problems) {
        boolean all = isKeyword(spec, ALL);
        Set<String> wanted = new HashSet<>();
        if (!all) {
            spec.getAsJsonArray().forEach(name -> wanted.add(name.getAsString()));
        }
        for (Skill skill : RegistrySkills.SKILLS_REGISTRY.getValues()) {
            boolean requested = wanted.remove(skill.getName());
            if (!all && !requested || !RegistrySkills.isEnabled(skill)) {
                continue;
            }
            if (skill.getToggle(player)) {
                toggle(capability, skill, true);
            } else if (requested) {
                problems.add("skill " + skill.getName() + " is still locked by its aptitude");
            }
        }
        wanted.forEach(name -> problems.add("unknown skill " + name));
    }

    static void sync(ServerPlayer player) {
        SyncAptitudeCapabilityCP.send(player);
    }

    static JsonObject captureAptitudes(ServerPlayer player, AptitudeCapability capability) {
        JsonObject aptitudes = new JsonObject();
        for (Aptitude aptitude : RegistryAptitudes.APTITUDES_REGISTRY.getValues()) {
            String name = aptitude.getName();
            aptitudes.addProperty(name, capability.getAptitudeLevel(aptitude) - InnateAptitudeBonusPower.getBonus(player, name));
        }
        return aptitudes;
    }

    static JsonObject capturePassives(AptitudeCapability capability) {
        JsonObject passives = new JsonObject();
        for (Passive passive : RegistryPassives.PASSIVES_REGISTRY.getValues()) {
            int level = capability.getPassiveLevel(passive);
            if (level > 0) {
                passives.addProperty(passive.getName(), level);
            }
        }
        return passives;
    }

    static JsonArray captureSkills(AptitudeCapability capability) {
        JsonArray skills = new JsonArray();
        for (Skill skill : RegistrySkills.SKILLS_REGISTRY.getValues()) {
            if (capability.getToggleSkill(skill)) {
                skills.add(skill.getName());
            }
        }
        return skills;
    }

    private static int allowedLevel(AptitudeCapability capability, Passive passive) {
        int aptitude = capability.getAptitudeLevel(passive.aptitude);
        int level = 0;
        while (level < passive.levelsRequired.length && aptitude >= passive.levelsRequired[level]) {
            level++;
        }
        return level;
    }

    private static void toggle(AptitudeCapability capability, Skill skill, boolean enabled) {
        capability.setToggleSkill(skill, enabled);
    }

    private static boolean isKeyword(JsonElement spec, String keyword) {
        return spec.isJsonPrimitive() && keyword.equals(spec.getAsString());
    }
}
