package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.component.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Emulates an Apotheosis attribute affix without depending on Apotheosis: a single attribute modifier
 * stored in the {@code raven_dnd_origins:masterwork} component under a per-item modifier id, so two
 * masterwork items touching the same attribute stack instead of evicting each other.
 */
public final class MasterworkAffixNbt {

    public static final ResourceLocation MASTERWORK_AFFIX_ID = RavenDndOrigins.loc("masterwork");
    private static final String MASTERWORK_MODIFIER_PATH = "affix/masterwork/";

    private MasterworkAffixNbt() {
    }

    public static ResourceLocation modifierId(UUID id) {
        return RavenDndOrigins.loc(MASTERWORK_MODIFIER_PATH + id);
    }

    public static boolean hasMasterwork(ItemStack stack) {
        return stack.has(ModDataComponents.MASTERWORK.get());
    }

    @Nullable
    public static ModDataComponents.Masterwork getMasterwork(ItemStack stack) {
        return stack.get(ModDataComponents.MASTERWORK.get());
    }

    public static void clearMasterwork(ItemStack stack) {
        stack.remove(ModDataComponents.MASTERWORK.get());
    }

    public static void putMasterwork(ItemStack stack, Holder<Attribute> attribute, AttributeModifier.Operation operation, double value) {
        ModDataComponents.Masterwork existing = getMasterwork(stack);
        UUID id = existing == null ? UUID.randomUUID() : existing.id();
        stack.set(ModDataComponents.MASTERWORK.get(), new ModDataComponents.Masterwork(id, attribute, operation, value));
    }

    @Nullable
    public static AttributeModifier readAsModifier(ItemStack stack) {
        ModDataComponents.Masterwork mw = getMasterwork(stack);
        if (mw == null) {
            return null;
        }
        return new AttributeModifier(modifierId(mw.id()), mw.value(), mw.operation());
    }

    @Nullable
    public static Holder<Attribute> readAttribute(ItemStack stack) {
        ModDataComponents.Masterwork mw = getMasterwork(stack);
        return mw == null ? null : mw.attribute();
    }
}
