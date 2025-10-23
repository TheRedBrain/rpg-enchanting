package com.github.theredbrain.rpgenchanting.screen;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.network.packet.RPGEnchantItemPacket;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.slotcustomizationapi.api.SlotCustomization;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantmentScreenHandler extends ScreenHandler {
	public final Inventory inventory = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			RPGEnchantmentScreenHandler.this.onContentChanged(this);
		}
	};
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> advancement_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> block_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> book_enchantments = new ArrayList<>();
	public final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> prefix_enchantments = new HashSet<>();
	public final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> suffix_enchantments = new HashSet<>();
	public final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> consumable_enchantments = new HashSet<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = new ArrayList<>();
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = null;
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = null;
	public int[] existing_enchantment_costs = new int[]{0, 0, 0, 0};
	public final World world;
	public final BlockPos blockPos;
	public final PlayerEntity player;

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, RPGEnchanterBlockData data) {
		this(syncId, playerInventory, data.blockPos, data.bookCost, data.enchantmentUnlockMode, data.advancement_enchantments, data.block_enchantments, data.book_enchantments);
	}

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos blockPos, RPGEnchantingTableBlock.BookCost bookCost, RPGEnchantingTableBlock.EnchantmentUnlockMode enchantmentUnlockMode, Set<MutablePair<String, Integer>> advancement_enchantments, Set<MutablePair<String, Integer>> block_enchantments, Set<MutablePair<String, Integer>> book_enchantments) {
		super(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, syncId);
		this.world = playerInventory.player.getEntityWorld();
		this.blockPos = blockPos;
		this.player = playerInventory.player;
		this.addSlot(new Slot(this.inventory, 0, 134, 40) {
			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.inventory, 1, 134, 62) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.prefix_item_cost.get())) || stack.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.suffix_item_cost.get()));
			}

			@Override
			public boolean isEnabled() {
				ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
				return super.isEnabled() && (serverConfig.old_enchantment_item_cost_multiplier.get() > 0.0 || serverConfig.new_enchantment_item_cost_multiplier.get() > 0.0) && (!serverConfig.prefix_item_cost.get().equals(Identifier.of("minecraft:air")) || !serverConfig.suffix_item_cost.get().equals(Identifier.of("minecraft:air")));
			}

		});

		int i;
		// hotbar 0 - 8
		for (i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInventory, i, 62 + i * 18, 209));
		}
		// main inventory 9 - 35
		for (i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlot(new Slot(playerInventory, j + (i + 1) * 9, 62 + j * 18, 151 + i * 18));
			}
		}

		// Inventory Size Attributes compatibility
		int activeHotbarSize = RPGEnchanting.getActiveHotbarSize(playerInventory.player);
		int activeInventorySize = RPGEnchanting.getActiveInventorySize(playerInventory.player);
		for (i = 0; i < 9; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= activeHotbarSize);
		}
		for (i = 9; i < 36; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= 9 + activeInventorySize);
		}

		this.updateEnchantmentLists(bookCost, enchantmentUnlockMode, advancement_enchantments, block_enchantments, book_enchantments);

	}

	public void updateEnchantmentLists(RPGEnchantingTableBlock.BookCost bookCost, RPGEnchantingTableBlock.EnchantmentUnlockMode enchantmentUnlockMode, Set<MutablePair<String, Integer>> advancement_enchantments, Set<MutablePair<String, Integer>> block_enchantments, Set<MutablePair<String, Integer>> book_enchantments) {

		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> verified_advancement_enchantments = new ArrayList<>();
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> verified_block_enchantments = new ArrayList<>();
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> verified_book_enchantments = new ArrayList<>();

		Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
		if (optionalEnchantmentRegistry.isPresent()) {

			for (MutablePair<String, Integer> pair : advancement_enchantments) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					verified_advancement_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}
			for (MutablePair<String, Integer> pair : block_enchantments) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					verified_block_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}
			for (MutablePair<String, Integer> pair : book_enchantments) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					verified_book_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}
		}

		if (enchantmentUnlockMode == RPGEnchantingTableBlock.EnchantmentUnlockMode.BLOCK_REQUIRED_FOR_ADVANCEMENT) {
			for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : verified_book_enchantments) {

				if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
					this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
					this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (bookCost != RPGEnchantingTableBlock.BookCost.KEEP) {
					this.consumable_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
			}
			for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : verified_advancement_enchantments) {

				if (verified_block_enchantments.contains(pair)) {

					if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
						this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
						this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					this.consumable_enchantments.remove(pair);
				}
			}
		} else if (enchantmentUnlockMode == RPGEnchantingTableBlock.EnchantmentUnlockMode.ADDITION) {
			for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : verified_book_enchantments) {

				if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
					this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
					this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (bookCost != RPGEnchantingTableBlock.BookCost.KEEP) {
					this.consumable_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
			}
			for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : verified_advancement_enchantments) {

				if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
					this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
					this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				this.consumable_enchantments.remove(pair);
			}
			for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : verified_block_enchantments) {

				if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
					this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
					this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
				}
				this.consumable_enchantments.remove(pair);
			}
		}
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		if (inventory == this.inventory) {
			this.existing_prefix_enchantment = null;
			this.existing_suffix_enchantment = null;
			this.current_prefix_enchantments.clear();
			this.current_suffix_enchantments.clear();
			this.existing_enchantment_costs = new int[]{0, 0, 0, 0};
			ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

			Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
			if (optionalEnchantmentRegistry.isPresent()) {

				ItemStack itemStack = inventory.getStack(0);
				ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.ENCHANTMENTS);
				if (!itemStack.isEmpty() && itemEnchantmentsComponent != null) {
					if (!itemEnchantmentsComponent.isEmpty()) {
						for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
							if (entry.getKey().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
								Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(entry.getKey().getIdAsString()));
								if (optionalEnchantmentReference.isPresent()) {
									this.existing_prefix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
									this.existing_enchantment_costs[0] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getMinPower(entry.getIntValue()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
									this.existing_enchantment_costs[1] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getAnvilCost() * entry.getIntValue() * serverConfig.old_enchantment_item_cost_multiplier.get()));
								}
							}
							if (entry.getKey().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
								Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(entry.getKey().getIdAsString()));
								if (optionalEnchantmentReference.isPresent()) {
									this.existing_suffix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
									this.existing_enchantment_costs[2] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getMinPower(entry.getIntValue()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
									this.existing_enchantment_costs[3] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getAnvilCost() * entry.getIntValue() * serverConfig.old_enchantment_item_cost_multiplier.get()));
								}
							}
						}
					}

					if (this.existing_prefix_enchantment == null || !this.existing_prefix_enchantment.getLeft().isIn(EnchantmentTags.CURSE)) {
						for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry : this.prefix_enchantments) {
							if (entry.getLeft().value().isAcceptableItem(itemStack)) {
								boolean bl = true;
								if (this.existing_prefix_enchantment != null) {
									bl = (entry.getLeft() != this.existing_prefix_enchantment.getLeft()) || (!Objects.equals(entry.getRight(), this.existing_prefix_enchantment.getRight()));
								}
								if (bl) {
									this.current_prefix_enchantments.add(new MutablePair<>(entry.getLeft(), entry.getRight()));
								}
							}
						}
					}
					if (this.existing_suffix_enchantment == null || !this.existing_suffix_enchantment.getLeft().isIn(EnchantmentTags.CURSE)) {
						for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry : this.suffix_enchantments) {
							if (entry.getLeft().value().isAcceptableItem(itemStack)) {
								boolean bl = true;
								if (this.existing_suffix_enchantment != null) {
									bl = (entry.getLeft() != this.existing_suffix_enchantment.getLeft()) || (!Objects.equals(entry.getRight(), this.existing_suffix_enchantment.getRight()));
								}
								if (bl) {
									this.current_suffix_enchantments.add(new MutablePair<>(entry.getLeft(), entry.getRight()));
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		ItemStack itemStack = this.inventory.getStack(0);

		if (itemStack.isEmpty()) {
			return false;
		}
		Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
		if (optionalEnchantmentRegistry.isEmpty()) {
			return false;
		}

		IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = optionalEnchantmentRegistry.get().getIndexedEntries();
		int first_threshold = this.current_prefix_enchantments.size();

		if (id >= 0 && id < first_threshold) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_prefix_enchantments.get(id);

			if (player instanceof ServerPlayerEntity serverPlayerEntity) {
				ServerPlayNetworking.send(serverPlayerEntity, new RPGEnchantItemPacket(
						this.blockPos,
						indexedIterable.getRawId(newEnchantment.getLeft()),
						newEnchantment.getRight(),
						this.consumable_enchantments.contains(newEnchantment),
						true
				));
			}
			return true;

		} else if (id >= first_threshold && id < first_threshold + this.current_suffix_enchantments.size()) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_suffix_enchantments.get(id - first_threshold);

			if (player instanceof ServerPlayerEntity serverPlayerEntity) {
				ServerPlayNetworking.send(serverPlayerEntity, new RPGEnchantItemPacket(
						this.blockPos,
						indexedIterable.getRawId(newEnchantment.getLeft()),
						newEnchantment.getRight(),
						this.consumable_enchantments.contains(newEnchantment),
						false
				));
			}
			return true;

		} else {
			Util.logErrorOrPause(player.getName() + " pressed invalid button id: " + id);
			return false;
		}
	}

	public int getPrefixItemCount() {
		ItemStack itemStack = this.inventory.getStack(1);
		return itemStack.isEmpty() || !itemStack.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.prefix_item_cost.get())) ? 0 : itemStack.getCount();
	}

	public int getSuffixItemCount() {
		ItemStack itemStack = this.inventory.getStack(1);
		return itemStack.isEmpty() || !itemStack.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.suffix_item_cost.get())) ? 0 : itemStack.getCount();
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.dropInventory(player, this.inventory);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot2 = this.slots.get(slot);
		if (slot2 != null && slot2.hasStack()) {
			ItemStack itemStack2 = slot2.getStack();
			itemStack = itemStack2.copy();
			if (slot == 0) {
				if (!this.insertItem(itemStack2, 2, 38, true)) {
					return ItemStack.EMPTY;
				}
			} else if (slot == 1) {
				if (!this.insertItem(itemStack2, 2, 38, true)) {
					return ItemStack.EMPTY;
				}
			} else if (itemStack2.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.prefix_item_cost.get())) || itemStack2.isOf(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.suffix_item_cost.get()))) {
				if (!this.insertItem(itemStack2, 1, 2, true)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (this.slots.get(0).hasStack() || !this.slots.get(0).canInsert(itemStack2)) {
					return ItemStack.EMPTY;
				}

				ItemStack itemStack3 = itemStack2.copyWithCount(1);
				itemStack2.decrement(1);
				this.slots.get(0).setStack(itemStack3);
			}

			if (itemStack2.isEmpty()) {
				slot2.setStack(ItemStack.EMPTY);
			} else {
				slot2.markDirty();
			}

			if (itemStack2.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot2.onTakeItem(player, itemStack2);
		}

		return itemStack;
	}

	public record RPGEnchanterBlockData(
			BlockPos blockPos,
			RPGEnchantingTableBlock.BookCost bookCost,
			RPGEnchantingTableBlock.EnchantmentUnlockMode enchantmentUnlockMode,
			Set<MutablePair<String, Integer>> advancement_enchantments,
			Set<MutablePair<String, Integer>> block_enchantments,
			Set<MutablePair<String, Integer>> book_enchantments
	) {

		public static final PacketCodec<RegistryByteBuf, RPGEnchanterBlockData> PACKET_CODEC = PacketCodec.of(RPGEnchanterBlockData::write, RPGEnchanterBlockData::new);

		public RPGEnchanterBlockData(RegistryByteBuf registryByteBuf) {
			this(
					registryByteBuf.readBlockPos(),
					RPGEnchantingTableBlock.BookCost.byName(registryByteBuf.readString()).orElse(RPGEnchantingTableBlock.BookCost.KEEP),
					RPGEnchantingTableBlock.EnchantmentUnlockMode.byName(registryByteBuf.readString()).orElse(RPGEnchantingTableBlock.EnchantmentUnlockMode.ADDITION),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER)
			);
		}

		private void write(RegistryByteBuf registryByteBuf) {
			registryByteBuf.writeBlockPos(blockPos);
			registryByteBuf.writeString(bookCost.asString());
			registryByteBuf.writeString(enchantmentUnlockMode.asString());
			registryByteBuf.writeCollection(this.advancement_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
			registryByteBuf.writeCollection(this.block_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
			registryByteBuf.writeCollection(this.book_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
		}
	}

}
