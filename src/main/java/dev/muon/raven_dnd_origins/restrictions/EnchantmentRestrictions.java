package dev.muon.raven_dnd_origins.restrictions;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginLayers;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantmentRestrictions {
    private static final Map<ResourceLocation, String> ENCHANTMENT_CLASS_MAP = new HashMap<>();

    static {
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("mending"), "artificer");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("sweeping_edge"), "barbarian");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("thorns"), "druid");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("sharpness"), "fighter");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("feather_falling"), "monk");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("smite"), "paladin");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("power"), "rogue");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.withDefaultNamespace("infinity"), "ranger");
        ENCHANTMENT_CLASS_MAP.put(ResourceLocation.fromNamespaceAndPath("apothic_enchanting", "endless_quiver"), "ranger");
    }

    /**
     * Whether {@code player} gets the effect of {@code enchantmentId} on {@code stack}. The stack is the item
     * carrying the enchantment; add-ons (Raven Apoth attunement) hook this to exempt individual items.
     */
    public static boolean isEnchantmentAllowed(Player player, ItemStack stack, @Nullable ResourceLocation enchantmentId) {
        if (!RavenDndOriginsConfig.enableEnchantmentRestrictions() || enchantmentId == null) {
            return true;
        }
        String requiredClass = ENCHANTMENT_CLASS_MAP.get(enchantmentId);
        if (requiredClass == null) {
            return true;
        }
        PlayerOriginsImpl origins = PlayerOriginsAttachment.get(player);
        if (origins == null) {
            return true;
        }
        ResourceLocation classLayer = RavenDndOrigins.loc("class");
        if (OriginLayers.get(classLayer) == null) {
            return true;
        }
        return origins.getOrigin(classLayer).equals(RavenDndOrigins.loc("class/" + requiredClass));
    }

    public static boolean isEnchantmentAllowed(Player player, ItemStack stack, @Nullable ResourceKey<Enchantment> key) {
        return isEnchantmentAllowed(player, stack, key == null ? null : key.location());
    }

    public static boolean isEnchantmentAllowed(Player player, ItemStack stack, @Nullable Holder<Enchantment> enchantment) {
        return isEnchantmentAllowed(player, stack, idOf(enchantment));
    }

    @Nullable
    public static String getRequiredClass(@Nullable ResourceLocation enchantmentId) {
        return enchantmentId == null ? null : ENCHANTMENT_CLASS_MAP.get(enchantmentId);
    }

    @Nullable
    public static String getRequiredClass(@Nullable Holder<Enchantment> enchantment) {
        return getRequiredClass(idOf(enchantment));
    }

    /** Display names of the vanilla enchantments a class unlocks; built from the id so no registry access is needed. */
    public static List<Component> getEnchantmentTextForClass(String className) {
        List<Component> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, String> entry : ENCHANTMENT_CLASS_MAP.entrySet()) {
            if (!entry.getValue().equals(className)) continue;
            if (!entry.getKey().getNamespace().equals("minecraft")) continue;
            out.add(Component.translatable(Util.makeDescriptionId("enchantment", entry.getKey())));
        }
        return out;
    }

    @Nullable
    private static ResourceLocation idOf(@Nullable Holder<Enchantment> enchantment) {
        if (enchantment == null) {
            return null;
        }
        return enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
    }
}
