package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.BeginReselectionMessage;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.apace100.origins.screen.ViewOriginScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ViewOriginScreen.class)
public abstract class ViewOriginScreenMixin extends OriginDisplayScreen {

    @Shadow(remap = false) private Button chooseOriginButton;
    @Shadow(remap = false) @Final private ArrayList<Tuple<Holder<OriginLayer>, Holder<Origin>>> originLayers;
    @Shadow(remap = false) private int currentLayer;

    protected ViewOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    /**
     * Re-routes the "Choose Origin" button to a single-layer reselection session, so an
     * in-inventory re-pick goes through the same persisted-session flow as the orbs.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void otherworld$redirectChooseButton(CallbackInfo ci) {
        if (this.chooseOriginButton == null || this.originLayers.isEmpty()) {
            return;
        }

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
                            current.getA().unwrapKey().ifPresent(key ->
                                    OtherworldOrigins.CHANNEL.sendToServer(
                                            new BeginReselectionMessage(List.of(key.location()))));
                        }
                ).bounds(bx, by, bw, bh).build()
        );
        this.chooseOriginButton.active = wasActive;
        this.chooseOriginButton.visible = wasVisible;
    }
}
