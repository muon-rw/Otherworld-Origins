package dev.muon.otherworldorigins.mixin.origins_patches;

import com.google.common.collect.Lists;
import dev.muon.otherworldorigins.client.screen.OtherworldOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.apace100.origins.screen.ViewOriginScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Objects;

@Mixin(value = ViewOriginScreen.class, remap = false)
public abstract class ViewOriginScreenMixin extends OriginDisplayScreen {

    @Shadow(remap = false) private Button chooseOriginButton;
    @Shadow(remap = false) @Final private ArrayList<Tuple<Holder<OriginLayer>, Holder<Origin>>> originLayers;
    @Shadow(remap = false) private int currentLayer;

    protected ViewOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void otherworld$redirectChooseButton(CallbackInfo ci) {
        if (this.chooseOriginButton == null || this.originLayers.isEmpty()) return;

        int bx = this.chooseOriginButton.getX();
        int by = this.chooseOriginButton.getY();
        int bw = this.chooseOriginButton.getWidth();
        int bh = this.chooseOriginButton.getHeight();
        boolean wasActive = this.chooseOriginButton.active;
        boolean wasVisible = this.chooseOriginButton.visible;

        this.removeWidget(this.chooseOriginButton);

        this.chooseOriginButton = this.addRenderableWidget(
                Button.builder(
                        Component.translatable("origins.gui.choose"),
                        b -> {
                            Tuple<Holder<OriginLayer>, Holder<Origin>> current = this.originLayers.get(this.currentLayer);
                            Minecraft.getInstance().setScreen(
                                    new OtherworldOriginScreen(Lists.newArrayList(current.getA()), 0, false)
                            );
                        }
                ).bounds(bx, by, bw, bh).build()
        );
        this.chooseOriginButton.active = wasActive;
        this.chooseOriginButton.visible = wasVisible;
    }
}
