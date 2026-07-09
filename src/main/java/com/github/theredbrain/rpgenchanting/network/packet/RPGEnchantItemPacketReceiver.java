package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Optional;

public class RPGEnchantItemPacketReceiver implements ServerPlayNetworking.PlayPayloadHandler<RPGEnchantItemPacket> {
	@Override
	public void receive(RPGEnchantItemPacket payload, ServerPlayNetworking.Context context) {

		ServerPlayer player = context.player();

		Level world = player.level();

		AbstractContainerMenu screenHandler = player.containerMenu;

		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		BlockPos blockPos = payload.blockPos();

		int enchantmentId = payload.enchantmentId();

		int enchantmentLevel = payload.enchantmentLevel();

		boolean shouldConsumeBook = payload.shouldConsumeBook();

		boolean isPrefix = payload.isPrefix();

		BlockEntity blockEntity = world.getBlockEntity(blockPos);

		if (screenHandler instanceof RPGEnchantmentScreenHandler rpgEnchantmentScreenHandler && blockEntity instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
			ItemStack enchantedItemStack = rpgEnchantmentScreenHandler.inventory.getItem(0);
			ItemStack itemCostItemStack = rpgEnchantmentScreenHandler.inventory.getItem(1);

			Optional<Registry<Enchantment>> optionalEnchantmentRegistry = world.registryAccess().lookup(Registries.ENCHANTMENT);

			if (optionalEnchantmentRegistry.isPresent()) {
				Optional<Holder.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().get(enchantmentId);

				if (optionalEnchantmentReference.isPresent()) {

					MutablePair<Holder.Reference<Enchantment>, Integer> newEnchantment = new MutablePair<>(optionalEnchantmentReference.get(), enchantmentLevel);
					int experience_cost_amount = (isPrefix ? rpgEnchantmentScreenHandler.existing_enchantment_costs[0] : rpgEnchantmentScreenHandler.existing_enchantment_costs[2]) + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getMaxCost(newEnchantment.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));

					if (player.experienceLevel >= experience_cost_amount || player.hasInfiniteMaterials()) {

						int item_cost_amount = (isPrefix ? rpgEnchantmentScreenHandler.existing_enchantment_costs[1] : rpgEnchantmentScreenHandler.existing_enchantment_costs[3]) + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getAnvilCost() * newEnchantment.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (((isPrefix ? itemCostItemStack.is(BuiltInRegistries.ITEM.getValue(serverConfig.prefix_item_cost.get())) : itemCostItemStack.is(BuiltInRegistries.ITEM.getValue(serverConfig.suffix_item_cost.get()))) && itemCostItemStack.getCount() >= item_cost_amount) || player.hasInfiniteMaterials()) {

							boolean shouldEnchant = true;

							if (shouldConsumeBook) {
								shouldEnchant = rpgEnchantingTableBlockEntity.applyBookCost(newEnchantment);
							}

							if (shouldEnchant) {

								player.onEnchantmentPerformed(enchantedItemStack, experience_cost_amount);
								ItemEnchantments.Mutable itemEnchantmentsComponentBuilder = new ItemEnchantments.Mutable(enchantedItemStack.getEnchantments());

								if (isPrefix && rpgEnchantmentScreenHandler.existing_prefix_enchantment != null) {
									itemEnchantmentsComponentBuilder.set(rpgEnchantmentScreenHandler.existing_prefix_enchantment.getLeft(), 0);
								} else if (!isPrefix && rpgEnchantmentScreenHandler.existing_suffix_enchantment != null) {
									itemEnchantmentsComponentBuilder.set(rpgEnchantmentScreenHandler.existing_suffix_enchantment.getLeft(), 0);
								}

								itemEnchantmentsComponentBuilder.upgrade(newEnchantment.getLeft(), newEnchantment.getRight());
								enchantedItemStack.set(DataComponents.ENCHANTMENTS, itemEnchantmentsComponentBuilder.toImmutable());

								enchantedItemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);

								TooltipDisplay tooltipDisplayComponent = enchantedItemStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
								tooltipDisplayComponent.withHidden(DataComponents.ENCHANTMENTS, true);
								enchantedItemStack.set(DataComponents.TOOLTIP_DISPLAY, tooltipDisplayComponent);

								if (serverConfig.enable_enchanted_by_player_component_application.get()) {
									enchantedItemStack.set(RPGEnchanting.PLAYER_ENCHANTED, ResolvableProfile.createResolved(player.getGameProfile()));
								}

								itemCostItemStack.consume(item_cost_amount, player);

								if (itemCostItemStack.isEmpty()) {
									rpgEnchantmentScreenHandler.inventory.setItem(1, ItemStack.EMPTY);
								}

								player.awardStat(Stats.ENCHANT_ITEM);
								CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer) player, enchantedItemStack, experience_cost_amount);

								rpgEnchantmentScreenHandler.inventory.setChanged();
								rpgEnchantmentScreenHandler.slotsChanged(rpgEnchantmentScreenHandler.inventory);

								world.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.1F + 0.9F);

							}
						}
					}
				}
			}
		}
	}
}