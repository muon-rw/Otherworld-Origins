package dev.muon.raven_dnd_origins.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class KitItems {
    private static final EquipmentSlot[] EQUIPMENT = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private KitItems() {}

    static boolean present(JsonObject kit) {
        return kit.has("equipment") || kit.has("curios") || kit.has("inventory");
    }

    static void clear(ServerPlayer player) {
        player.getInventory().clearContent();
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> curios.getCurios().values().forEach(handler -> {
            empty(handler.getStacks());
            empty(handler.getCosmeticStacks());
        }));
    }

    // Main hand first, so inventory items fill the other hotbar slots around it
    static void give(ServerPlayer player, JsonObject kit, List<String> problems) {
        RegistryOps<JsonElement> ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonObject equipment = GsonHelper.getAsJsonObject(kit, "equipment", new JsonObject());
        for (EquipmentSlot slot : EQUIPMENT) {
            if (equipment.has(slot.getName())) {
                player.setItemSlot(slot, decode(ops, equipment.get(slot.getName()), problems));
            }
        }
        JsonObject curios = GsonHelper.getAsJsonObject(kit, "curios", new JsonObject());
        if (!curios.isEmpty()) {
            Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(player);
            if (inventory.isEmpty()) {
                problems.add("player has no curios inventory");
            }
            inventory.ifPresent(handler -> giveCurios(handler, curios, ops, problems));
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(kit, "inventory", new JsonArray())) {
            ItemStack stack = decode(ops, element, problems);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                problems.add("inventory full, left out " + stack);
            }
        }
    }

    static void capture(ServerPlayer player, JsonObject kit) {
        RegistryOps<JsonElement> ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonObject equipment = new JsonObject();
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                equipment.add(slot.getName(), encode(ops, stack));
            }
        }
        kit.add("equipment", equipment);

        JsonObject curios = new JsonObject();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().forEach((id, handler) -> {
            JsonArray stacks = captureSlots(ops, handler);
            if (!stacks.isEmpty()) {
                curios.add(id, stacks);
            }
        }));
        kit.add("curios", curios);

        JsonArray inventory = new JsonArray();
        List<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty() && i != player.getInventory().selected) {
                inventory.add(encode(ops, items.get(i)));
            }
        }
        kit.add("inventory", inventory);
    }

    private static void giveCurios(ICuriosItemHandler curios, JsonObject spec, RegistryOps<JsonElement> ops, List<String> problems) {
        for (Map.Entry<String, JsonElement> entry : spec.entrySet()) {
            ICurioStacksHandler handler = curios.getCurios().get(entry.getKey());
            if (handler == null) {
                problems.add("no curio slot type " + entry.getKey());
                continue;
            }
            JsonArray stacks = entry.getValue().getAsJsonArray();
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = decode(ops, stacks.get(i), problems);
                if (i < handler.getSlots()) {
                    handler.getStacks().setStackInSlot(i, stack);
                } else if (!stack.isEmpty()) {
                    problems.add("only " + handler.getSlots() + " " + entry.getKey() + " slot(s), left out " + stack);
                }
            }
        }
    }

    // Trailing empty slots are dropped; inner ones are kept so each item stays in its slot
    private static JsonArray captureSlots(RegistryOps<JsonElement> ops, ICurioStacksHandler handler) {
        JsonArray stacks = new JsonArray();
        int last = -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStacks().getStackInSlot(i).isEmpty()) {
                last = i;
            }
        }
        for (int i = 0; i <= last; i++) {
            stacks.add(encode(ops, handler.getStacks().getStackInSlot(i)));
        }
        return stacks;
    }

    private static ItemStack decode(RegistryOps<JsonElement> ops, JsonElement json, List<String> problems) {
        return ItemStack.OPTIONAL_CODEC.parse(ops, json).resultOrPartial(error -> problems.add("item " + json + ": " + error)).orElse(ItemStack.EMPTY);
    }

    private static JsonElement encode(RegistryOps<JsonElement> ops, ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).getOrThrow();
    }

    private static void empty(IItemHandlerModifiable handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
