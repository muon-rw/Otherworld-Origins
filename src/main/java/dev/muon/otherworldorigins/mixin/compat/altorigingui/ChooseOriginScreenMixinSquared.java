package dev.muon.otherworldorigins.mixin.compat.altorigingui;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = ChooseOriginScreen.class, priority = 1500)
public abstract class ChooseOriginScreenMixinSquared extends OriginDisplayScreen {

    @Shadow(remap = false)
    @Final
    private List<Holder<Origin>> originSelection;

    public ChooseOriginScreenMixinSquared(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    /** See {@link OriginDisplayScreenMixin#otherworldorigins$resolveNamespace} */
    @Unique
    private static String otherworldorigins$resolveNamespace(String spellName) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY.get().getValues()) {
            if (spell.getSpellResource().getPath().equals(spellName)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }

    @TargetHandler(
            mixin = "me.ultrusmods.altorigingui.mixin.ChooseOriginScreenMixinForge",
            name = "renderOriginChoicesBox"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V")
    )
    private void otherworldorigins$renderCantripTabIcon(
            GuiGraphics graphics, ItemStack stack, int x, int y,
            Operation<Void> original,
            @Local Origin origin
    ) {
        String spellName = otherworldorigins$getSpellName(origin);
        if (spellName != null) {
            String namespace = otherworldorigins$resolveNamespace(spellName);
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                    namespace, "textures/gui/spell_icons/" + spellName + ".png");
            graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        original.call(graphics, stack, x, y);
    }

    @Unique
    private String otherworldorigins$getSpellName(Origin origin) {
        for (Holder<Origin> holder : this.originSelection) {
            if (holder.isBound() && holder.get() == origin) {
                return holder.unwrapKey()
                        .map(key -> {
                            String path = key.location().getPath();
                            if (path.startsWith("cantrips/two/")) {
                                return path.substring("cantrips/two/".length());
                            } else if (path.startsWith("cantrips/")) {
                                return path.substring("cantrips/".length());
                            }
                            return null;
                        })
                        .orElse(null);
            }
        }
        return null;
    }
}
