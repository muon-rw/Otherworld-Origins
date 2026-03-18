package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpellRegistry.class, remap = false)
public class SpellRegistryMixin {

    @ModifyReturnValue(method = "lambda$static$0", at = @At("RETURN"))
    private static RegistryBuilder<?> otherworldorigins$enableSpellTags(RegistryBuilder<?> builder) {
        return builder.hasTags();
    }
}
