package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(Player.class)
public class PlayerMixin implements IEnchantmentSeedResettable {

    // also see ench_restrictions.PlayerMixin

    @Shadow(remap = true)
    protected int enchantmentSeed;

    @Override
    public void otherworld$resetEnchantmentSeed() {
        this.enchantmentSeed = ((Player) (Object) this).getRandom().nextInt();
    }

}
