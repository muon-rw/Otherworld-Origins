package dev.muon.otherworldorigins.client.compat.goblinstyranny;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.muon.otherworld.power.PowerPresenceCache;
import dev.muon.otherworldorigins.OtherworldOrigins;
import goblinstyranny.client.renderer.FakeGoblinEarsArmorRenderer;
import goblinstyranny.init.GoblinsTyrannyModItems;
import goblinstyranny.item.FakeGoblinEarsItem;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Renders Goblin's Tyranny fake-ear geo on goblin / hobgoblin origin players when they are not
 * already wearing the mod's fake ears helmet (which has its own renderer).
 */
@OnlyIn(Dist.CLIENT)
public class GoblinKinFakeEarsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation FAKE_EARS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("goblins_tyranny", "textures/item/fakeears.png");

    private final FakeGoblinEarsArmorRenderer earsRenderer = new FakeGoblinEarsArmorRenderer();
    private ItemStack cosmeticStack;

    public GoblinKinFakeEarsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static boolean shouldRender(AbstractClientPlayer player) {
        IPowerContainer container = PowerPresenceCache.getContainer(player);
        if (container == null || !container.hasPower(OtherworldOrigins.GOBLINS_TYRANNY_KIN_POWER)) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof FakeGoblinEarsItem item && item.getType() == ArmorItem.Type.HELMET) {
            return false;
        }
        return true;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!shouldRender(player)) {
            return;
        }
        if (cosmeticStack == null) {
            cosmeticStack = new ItemStack(GoblinsTyrannyModItems.FAKE_GOBLIN_EARS_HELMET.get(), 1);
        }
        FakeGoblinEarsItem earsItem = (FakeGoblinEarsItem) cosmeticStack.getItem();
        earsRenderer.prepForRender(player, cosmeticStack, EquipmentSlot.HEAD, getParentModel());

        var renderType = earsRenderer.getRenderType(earsItem, FAKE_EARS_TEXTURE, buffer, partialTick);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        earsRenderer.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }
}
