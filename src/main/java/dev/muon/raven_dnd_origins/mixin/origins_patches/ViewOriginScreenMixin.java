package dev.muon.raven_dnd_origins.mixin.origins_patches;

import dev.muon.raven_dnd_origins.network.BeginReselectionMessage;
import dev.overgrown.origins.client.screen.OriginDisplayScreen;
import dev.overgrown.origins.client.screen.ViewOriginScreen;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ViewOriginScreen.class, remap = false)
public abstract class ViewOriginScreenMixin extends OriginDisplayScreen {

    @Shadow private Button chooseOriginButton;
    @Shadow @Final private ArrayList<Tuple<OriginLayer, Origin>> originLayers;
    @Shadow private int currentLayer;

    protected ViewOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    /**
     * Re-routes the "Choose Origin" button to a single-layer reselection session, so an
     * in-inventory re-pick goes through the same persisted-session flow as the orbs.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void raven_dnd_origins$redirectChooseButton(CallbackInfo ci) {
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
                            OriginLayer layer = this.originLayers.get(this.currentLayer).getA();
                            PacketDistributor.sendToServer(new BeginReselectionMessage(List.of(layer.id())));
                        }
                ).bounds(bx, by, bw, bh).build()
        );
        this.chooseOriginButton.active = wasActive;
        this.chooseOriginButton.visible = wasVisible;
    }
}
