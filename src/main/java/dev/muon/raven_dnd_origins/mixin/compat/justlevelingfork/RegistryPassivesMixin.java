package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.seniors.justlevelingfork.registry.FabricRegistryRef;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistryItems;
import com.seniors.justlevelingfork.registry.RegistryPassives;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import com.seniors.justlevelingfork.registry.passive.Passive;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.attribute.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Swaps two of JustLevelingFork's built-in passives: max_health drives our health_per_level attribute
 * (scaled by character level) instead of vanilla max health, and attack_speed moves to Dexterity.
 */
@Mixin(value = RegistryPassives.class, remap = false)
public class RegistryPassivesMixin {

    @Inject(
            method = "register(Ljava/lang/String;Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;Lnet/minecraft/resources/ResourceLocation;Ljava/util/function/Supplier;Ljava/lang/String;Ljava/lang/Object;[I)Lcom/seniors/justlevelingfork/registry/FabricRegistryRef;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void replaceOriginsPassives(String name, Aptitude aptitude, ResourceLocation texture,
                                               Supplier<Holder<Attribute>> attribute, String attributeUuid,
                                               Object attributeValue, int[] levelsRequired,
                                               CallbackInfoReturnable<FabricRegistryRef<Passive>> cir) {
        if ("max_health".equals(name)) {
            cir.setReturnValue(raven_dnd_origins$register(name, new Passive(
                    RavenDndOrigins.loc("health_per_level"), aptitude, texture,
                    () -> ModAttributes.HEALTH_PER_LEVEL, attributeUuid,
                    raven_dnd_origins$healthPerLevelValue(), raven_dnd_origins$healthPerLevelLevels())));
        } else if ("attack_speed".equals(name)) {
            cir.setReturnValue(raven_dnd_origins$register(name, new Passive(
                    RegistryItems.id(name), RegistryAptitudes.DEXTERITY.get(), texture,
                    attribute, attributeUuid, attributeValue, levelsRequired)));
        }
    }

    @Inject(method = "refreshFromConfig", at = @At("RETURN"))
    private static void restoreHealthPerLevelValues(CallbackInfo ci) {
        RegistryPassives.MAX_HEALTH.get().rebind(
                raven_dnd_origins$healthPerLevelValue(), raven_dnd_origins$healthPerLevelLevels());
    }

    @Unique
    private static FabricRegistryRef<Passive> raven_dnd_origins$register(String name, Passive passive) {
        return new FabricRegistryRef<>(Registry.register(RegistryPassives.PASSIVES, RegistryItems.id(name), passive));
    }

    @Unique
    private static float raven_dnd_origins$healthPerLevelValue() {
        return 5.0f;
    }

    @Unique
    private static int[] raven_dnd_origins$healthPerLevelLevels() {
        return new int[]{4, 8, 12, 16, 20};
    }
}
