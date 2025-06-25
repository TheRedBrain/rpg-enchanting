package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class UpdateEnchantingScreenPacketReceiver implements ServerPlayNetworking.PlayPayloadHandler<UpdateEnchantingScreenPacket> {
	@Override
	public void receive(UpdateEnchantingScreenPacket payload, ServerPlayNetworking.Context context) {

		ServerPlayerEntity player = context.player();

		World world = player.getWorld();

		ScreenHandler screenHandler = player.currentScreenHandler;

		if (screenHandler instanceof RPGEnchantmentScreenHandler rpgEnchantmentScreenHandler) {
			ItemStack enchantedItemStack = rpgEnchantmentScreenHandler.inventory.getStack(0).copy();
			ItemStack itemCostItemStack = rpgEnchantmentScreenHandler.inventory.getStack(1).copy();

			BlockEntity blockEntity = world.getBlockEntity(rpgEnchantmentScreenHandler.blockPos);

			rpgEnchantmentScreenHandler.inventory.setStack(0, ItemStack.EMPTY);
			rpgEnchantmentScreenHandler.inventory.setStack(1, ItemStack.EMPTY);

			if (blockEntity instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
				player.openHandledScreen(RPGEnchantingTableBlock.createRPGEnchanterBlockScreenHandlerFactory(
						rpgEnchantingTableBlockEntity.getPos(),
						rpgEnchantingTableBlockEntity.getBookCost(),
						rpgEnchantingTableBlockEntity.getEnchantingMode(),
						rpgEnchantingTableBlockEntity.getAdvancementEnchantments(player),
						rpgEnchantingTableBlockEntity.getBlockEnchantments(),
						rpgEnchantingTableBlockEntity.getBookEnchantments()
				));
			}

			ScreenHandler newScreenHandler = player.currentScreenHandler;

			if (newScreenHandler instanceof RPGEnchantmentScreenHandler newRpgEnchantmentScreenHandler) {

				newRpgEnchantmentScreenHandler.inventory.setStack(0, enchantedItemStack);
				newRpgEnchantmentScreenHandler.inventory.setStack(1, itemCostItemStack);

			}
		}
	}
}