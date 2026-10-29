package dev.muon.raven_dnd_origins.mixin.compat.rpg_villagers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.CharismaPower;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.rpgvillagers.village.VillageGrudges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = VillageGrudges.class, remap = false)
public class VillageGrudgesMixin {

    // Rolling on the witness list means we only roll, and only show the message, when a villager actually saw it.
    @ModifyExpressionValue(method = "blame", at = @At(value = "INVOKE",
            target = "Lnet/pixeldreamstudios/rpgvillagers/village/VillageGrudges;witnesses(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"))
    private static List<Villager> raven_dnd_origins$charismaSaveFromGrudge(List<Villager> witnesses, @Local(argsOnly = true) Player player) {
        if (witnesses.isEmpty()) return witnesses;

        float charisma = CharismaPower.totalCharisma(player);
        if (charisma <= 0 || player.getRandom().nextFloat() >= Math.min(charisma, 1.0f)) return witnesses;

        player.displayClientMessage(Component.translatable("raven_dnd_origins.message.charisma_save"), true);
        return List.of();
    }
}
