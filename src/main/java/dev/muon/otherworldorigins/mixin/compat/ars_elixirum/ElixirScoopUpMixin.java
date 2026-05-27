package dev.muon.otherworldorigins.mixin.compat.ars_elixirum;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.compat.elixirum.ArsElixirHelper;
import dev.muon.otherworldorigins.power.SuppressElixirRiskPower;
import dev.obscuria.elixirum.common.alchemy.traits.Risk;
import dev.obscuria.elixirum.common.world.block.GlassCauldronInteraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * After Ars Elixirum's Glass Cauldron scoops an elixir into the player's hand, applies the
 * scooper's {@code modify_brewed_potion} bonuses (mirroring brewing-stand behavior) and the
 * {@code suppress_elixir_risk} capstone perk if active. Runs server-side via the wrapped
 * vanilla {@link Player#addItem(ItemStack)} call so the modification is visible to both
 * inventory-add and the fallback drop path.
 */
@Mixin(value = GlassCauldronInteraction.ElixirScoopUp.class , remap = false)
public abstract class ElixirScoopUpMixin {

    @WrapOperation(
            method = "interact",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addItem(Lnet/minecraft/world/item/ItemStack;)Z", remap = true)
    )
    private boolean otherworld$restampOnAdd(Player player, ItemStack result, Operation<Boolean> original) {
        Risk override = SuppressElixirRiskPower.has(player) ? Risk.PERFECT : null;
        ArsElixirHelper.restampElixir(player, result, override);
        return original.call(player, result);
    }
}
