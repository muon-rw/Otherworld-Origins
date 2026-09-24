package dev.muon.raven_dnd_origins.util.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Defers an action until a player's cast of a spell is over, including any Iron's recast sequence the cast opened.
 * Actions receive the current player entity, which differs from the caster after an End-exit respawn.
 */
public final class CastFinishCallbacks {

    private static final Map<UUID, Map<String, Consumer<ServerPlayer>>> PENDING = new HashMap<>();

    private CastFinishCallbacks() {
    }

    public static void runWhenFinished(ServerPlayer player, AbstractSpell spell, Consumer<ServerPlayer> action) {
        String spellId = spell.getSpellId();
        if (isFinished(player, spellId)) {
            action.accept(player);
            return;
        }
        PENDING.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).putIfAbsent(spellId, action);
    }

    public static void onCastComplete(ServerPlayer player, AbstractSpell spell) {
        runIfFinished(player, spell.getSpellId());
    }

    public static void onRecastSequenceEnd(ServerPlayer player, String spellId) {
        runIfFinished(player, spellId);
    }

    /**
     * Iron's saves open recasts across logout, but pending actions are not saved, so they run now.
     */
    public static void runAllPending(ServerPlayer player) {
        Map<String, Consumer<ServerPlayer>> byPlayer = PENDING.remove(player.getUUID());
        if (byPlayer != null) {
            byPlayer.values().forEach(action -> action.accept(player));
        }
    }

    private static void runIfFinished(ServerPlayer player, String spellId) {
        Map<String, Consumer<ServerPlayer>> byPlayer = PENDING.get(player.getUUID());
        if (byPlayer == null || !byPlayer.containsKey(spellId) || !isFinished(player, spellId)) {
            return;
        }
        Consumer<ServerPlayer> action = byPlayer.remove(spellId);
        if (byPlayer.isEmpty()) {
            PENDING.remove(player.getUUID());
        }
        action.accept(player);
    }

    private static boolean isFinished(ServerPlayer player, String spellId) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        boolean castingIt = magicData.isCasting() && spellId.equals(magicData.getCastingSpellId());
        return !castingIt && !magicData.getPlayerRecasts().hasRecastForSpell(spellId);
    }
}
