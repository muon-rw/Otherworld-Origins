package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void otherworldorigins$restrictFortuneDrops(BlockState pState, ServerLevel pLevel, BlockPos pPos, BlockEntity pBlockEntity, Entity pEntity, ItemStack pTool, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (pEntity instanceof Player player) {
            int fortuneLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.BLOCK_FORTUNE, pTool);
            if (fortuneLevel > 0 && !EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.BLOCK_FORTUNE)) {

                ItemStack newTool = pTool.copy();
                otherworld$removeFortuneEnchantment(newTool);

                LootParams.Builder lootParamsBuilder = new LootParams.Builder(pLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
                        .withParameter(LootContextParams.TOOL, newTool)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, pEntity)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, pBlockEntity);

                List<ItemStack> newDrops = pState.getDrops(lootParamsBuilder);
                cir.setReturnValue(newDrops);
            }
        }
    }

    @Unique
    private static void otherworld$removeFortuneEnchantment(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Enchantments", 9)) {
            ListTag enchantments = tag.getList("Enchantments", 10);
            ResourceLocation fortuneId = ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.BLOCK_FORTUNE);
            if (fortuneId != null) {
                String fortuneIdString = fortuneId.toString();
                for (int i = enchantments.size() - 1; i >= 0; i--) {
                    CompoundTag enchantmentTag = enchantments.getCompound(i);
                    if (enchantmentTag.getString("id").equals(fortuneIdString)) {
                        enchantments.remove(i);
                    }
                }
                if (enchantments.isEmpty()) {
                    tag.remove("Enchantments");
                }
            }
        }
    }
}