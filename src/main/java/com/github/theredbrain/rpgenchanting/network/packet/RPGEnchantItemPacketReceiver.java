package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Optional;

public class RPGEnchantItemPacketReceiver implements ServerPlayNetworking.PlayPayloadHandler<RPGEnchantItemPacket> {
	@Override
	public void receive(RPGEnchantItemPacket payload, ServerPlayNetworking.Context context) {

		ServerPlayerEntity player = context.player();

		World world = player.getEntityWorld();

		ScreenHandler screenHandler = player.currentScreenHandler;

		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		BlockPos blockPos = payload.blockPos();

		int enchantmentId = payload.enchantmentId();

		int enchantmentLevel = payload.enchantmentLevel();

		boolean shouldConsumeBook = payload.shouldConsumeBook();

		boolean isPrefix = payload.isPrefix();

		BlockEntity blockEntity = world.getBlockEntity(blockPos);

		if (screenHandler instanceof RPGEnchantmentScreenHandler rpgEnchantmentScreenHandler && blockEntity instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
			ItemStack enchantedItemStack = rpgEnchantmentScreenHandler.inventory.getStack(0);
			ItemStack itemCostItemStack = rpgEnchantmentScreenHandler.inventory.getStack(1);

			Optional<Registry<Enchantment>> optionalEnchantmentRegistry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);

			if (optionalEnchantmentRegistry.isPresent()) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(enchantmentId);

				if (optionalEnchantmentReference.isPresent()) {

					MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = new MutablePair<>(optionalEnchantmentReference.get(), enchantmentLevel);
					int experience_cost_amount = (isPrefix ? rpgEnchantmentScreenHandler.existing_enchantment_costs[0] : rpgEnchantmentScreenHandler.existing_enchantment_costs[2]) + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getMaxPower(newEnchantment.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));

					if (player.experienceLevel >= experience_cost_amount || player.isInCreativeMode()) {

						int item_cost_amount = (isPrefix ? rpgEnchantmentScreenHandler.existing_enchantment_costs[1] : rpgEnchantmentScreenHandler.existing_enchantment_costs[3]) + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getAnvilCost() * newEnchantment.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (((isPrefix ? itemCostItemStack.isOf(Registries.ITEM.get(serverConfig.prefix_item_cost.get())) : itemCostItemStack.isOf(Registries.ITEM.get(serverConfig.suffix_item_cost.get()))) && itemCostItemStack.getCount() >= item_cost_amount) || player.isInCreativeMode()) {

							boolean shouldEnchant = true;

							if (shouldConsumeBook) {
								shouldEnchant = rpgEnchantingTableBlockEntity.applyBookCost(newEnchantment);
							}

							if (shouldEnchant) {

								player.applyEnchantmentCosts(enchantedItemStack, experience_cost_amount);
								ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(enchantedItemStack.getEnchantments());

								if (isPrefix && rpgEnchantmentScreenHandler.existing_prefix_enchantment != null) {
									itemEnchantmentsComponentBuilder.set(rpgEnchantmentScreenHandler.existing_prefix_enchantment.getLeft(), 0);
								} else if (!isPrefix && rpgEnchantmentScreenHandler.existing_suffix_enchantment != null) {
									itemEnchantmentsComponentBuilder.set(rpgEnchantmentScreenHandler.existing_suffix_enchantment.getLeft(), 0);
								}

								itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
								enchantedItemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build());

								enchantedItemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);

								TooltipDisplayComponent tooltipDisplayComponent = enchantedItemStack.getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);
								tooltipDisplayComponent.with(DataComponentTypes.ENCHANTMENTS, true);
								enchantedItemStack.set(DataComponentTypes.TOOLTIP_DISPLAY, tooltipDisplayComponent);

								if (serverConfig.enable_enchanted_by_player_component_application.get()) {
									enchantedItemStack.set(RPGEnchanting.PLAYER_ENCHANTED, ProfileComponent.ofStatic(player.getGameProfile()));
								}

								itemCostItemStack.decrementUnlessCreative(item_cost_amount, player);

								if (itemCostItemStack.isEmpty()) {
									rpgEnchantmentScreenHandler.inventory.setStack(1, ItemStack.EMPTY);
								}

								player.incrementStat(Stats.ENCHANT_ITEM);
								Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, enchantedItemStack, experience_cost_amount);

								rpgEnchantmentScreenHandler.inventory.markDirty();
								rpgEnchantmentScreenHandler.onContentChanged(rpgEnchantmentScreenHandler.inventory);

								world.playSound(null, blockPos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);

							}
						}
					}
				}
			}
		}
	}
}