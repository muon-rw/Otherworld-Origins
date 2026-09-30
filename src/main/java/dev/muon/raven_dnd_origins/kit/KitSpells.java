package dev.muon.raven_dnd_origins.kit;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

final class KitSpells {
    private KitSpells() {}

    // Spells that need a learning drop would otherwise sit uncastable in the kit's books and imbued gear
    static void learnContained(ServerPlayer player) {
        SyncedSpellData spells = MagicData.getPlayerMagicData(player).getSyncedData();
        for (ItemStack stack : equippedStacks(player)) {
            if (!ISpellContainer.isSpellContainer(stack)) {
                continue;
            }
            for (SpellSlot slot : ISpellContainer.get(stack).getActiveSpells()) {
                AbstractSpell spell = slot.spellData().getSpell();
                if (!spells.isSpellLearned(spell)) {
                    spells.learnSpell(spell);
                }
            }
        }
    }

    private static List<ItemStack> equippedStacks(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stacks.add(player.getItemBySlot(slot));
        }
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> curios.getCurios().values().forEach(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                stacks.add(handler.getStacks().getStackInSlot(i));
            }
        }));
        return stacks;
    }
}
