package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UpdateEnchantingScreenPacketReceiver implements ServerPlayNetworking.PlayPayloadHandler<UpdateEnchantingScreenPacket> {
	@Override
	public void receive(UpdateEnchantingScreenPacket payload, ServerPlayNetworking.Context context) {

		ServerPlayer player = context.player();

		Level world = player.level();

		AbstractContainerMenu screenHandler = player.containerMenu;

		if (screenHandler instanceof RPGEnchantmentScreenHandler rpgEnchantmentScreenHandler) {
			ItemStack enchantedItemStack = rpgEnchantmentScreenHandler.inventory.getItem(0).copy();
			ItemStack itemCostItemStack = rpgEnchantmentScreenHandler.inventory.getItem(1).copy();

			BlockEntity blockEntity = world.getBlockEntity(rpgEnchantmentScreenHandler.blockPos);

			rpgEnchantmentScreenHandler.inventory.setItem(0, ItemStack.EMPTY);
			rpgEnchantmentScreenHandler.inventory.setItem(1, ItemStack.EMPTY);

			if (blockEntity instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
				player.openMenu(RPGEnchantingTableBlock.createRPGEnchanterBlockScreenHandlerFactory(
						rpgEnchantingTableBlockEntity.getBlockPos(),
						((Nameable)rpgEnchantingTableBlockEntity).getDisplayName(),
						rpgEnchantingTableBlockEntity.getBookCost(),
						rpgEnchantingTableBlockEntity.getEnchantingMode(),
						rpgEnchantingTableBlockEntity.getAdvancementEnchantments(player),
						rpgEnchantingTableBlockEntity.getBlockEnchantments(),
						rpgEnchantingTableBlockEntity.getBookEnchantments()
				));
			}

			AbstractContainerMenu newScreenHandler = player.containerMenu;

			if (newScreenHandler instanceof RPGEnchantmentScreenHandler newRpgEnchantmentScreenHandler) {

				newRpgEnchantmentScreenHandler.inventory.setItem(0, enchantedItemStack);
				newRpgEnchantmentScreenHandler.inventory.setItem(1, itemCostItemStack);

			}
		}
	}
}