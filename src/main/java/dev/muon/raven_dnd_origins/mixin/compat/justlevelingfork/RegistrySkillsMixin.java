package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.seniors.justlevelingfork.client.core.Value;
import com.seniors.justlevelingfork.client.core.ValueType;
import com.seniors.justlevelingfork.registry.FabricRegistryRef;
import com.seniors.justlevelingfork.registry.RegistryItems;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces JustLevelingFork's Diamond Skin with fixed 15% resistance (30% while sneaking) and no armor
 * amplifier; the resistance itself is applied by our damage pipeline, not by the vanilla effect.
 */
@Mixin(value = RegistrySkills.class, remap = false)
public class RegistrySkillsMixin {

    @Inject(
            method = "register(Ljava/lang/String;Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;ILnet/minecraft/resources/ResourceLocation;[Lcom/seniors/justlevelingfork/client/core/Value;)Lcom/seniors/justlevelingfork/registry/FabricRegistryRef;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void replaceDiamondSkin(String name, Aptitude aptitude, int requiredLvl, ResourceLocation texture,
                                           Value[] configValues, CallbackInfoReturnable<FabricRegistryRef<Skill>> cir) {
        if (!"diamond_skin".equals(name)) {
            return;
        }
        Skill skill = new Skill(RavenDndOrigins.loc("diamond_skin"), aptitude, requiredLvl, texture,
                raven_dnd_origins$diamondSkinValues());
        cir.setReturnValue(new FabricRegistryRef<>(
                Registry.register(RegistrySkills.SKILLS, RegistryItems.id(name), skill)));
    }

    @Inject(method = "refreshFromConfig", at = @At("RETURN"))
    private static void restoreDiamondSkinValues(CallbackInfo ci) {
        Skill diamondSkin = RegistrySkills.DIAMOND_SKIN.get();
        diamondSkin.rebind(diamondSkin.requiredLevel, raven_dnd_origins$diamondSkinValues());
    }

    @Unique
    private static Value[] raven_dnd_origins$diamondSkinValues() {
        return new Value[]{
                new Value(ValueType.PERCENT, 15),
                // JustLevelingFork feeds this to its armor bonus; we want none.
                new Value(ValueType.AMPLIFIER, 0),
                new Value(ValueType.PERCENT, 30)
        };
    }
}
