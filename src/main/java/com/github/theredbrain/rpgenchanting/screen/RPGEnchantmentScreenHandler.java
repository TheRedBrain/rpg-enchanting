package com.github.theredbrain.rpgenchanting.screen;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.slotcustomizationapi.api.SlotCustomization;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
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
	static final Identifier EMPTY_ITEM_COST_SLOT_TEXTURE = RPGEnchanting.identifier("item/empty_slot_item_cost");
	private final Inventory inventory = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			RPGEnchantmentScreenHandler.this.onContentChanged(this);
		}
	};
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> prefix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> suffix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = new ArrayList<>();
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = null;
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = null;
	public int[] existing_enchantment_costs = new int[]{0, 0, 0, 0};
	private final World world;
	private final BlockPos blockPos;
	public final PlayerEntity player;

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, RPGEnchanterBlockData data) {
		this(syncId, playerInventory, data.blockPos, data.prefix_enchantments, data.suffix_enchantments);
	}

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos blockPos, Set<MutablePair<String, Integer>> prefix_enchantments, Set<MutablePair<String, Integer>> suffix_enchantments) {
		super(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, syncId);
		this.world = playerInventory.player.getWorld();
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
				return stack.isIn(RPGEnchanting.ENCHANTING_PREFIX_COST_ITEMS) || stack.isIn(RPGEnchanting.ENCHANTING_SUFFIX_COST_ITEMS);
			}

			@Override
			public Pair<Identifier, Identifier> getBackgroundSprite() {
				return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, RPGEnchantmentScreenHandler.EMPTY_ITEM_COST_SLOT_TEXTURE);
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

		((SlotCustomization) this.slots.get(1)).slotcustomizationapi$setDisabledOverride(RPGEnchanting.SERVER_CONFIG.old_enchantment_item_cost_multiplier.get() > 0.0 || RPGEnchanting.SERVER_CONFIG.new_enchantment_item_cost_multiplier.get() > 0.0);

		// Inventory Size Attributes compatibility
		int activeHotbarSize = RPGEnchanting.getActiveHotbarSize(playerInventory.player);
		int activeInventorySize = RPGEnchanting.getActiveInventorySize(playerInventory.player);
		for (i = 0; i < 9; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= activeHotbarSize);
		}
		for (i = 9; i < 36; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= 9 + activeInventorySize);
		}

		for (MutablePair<String, Integer> pair : prefix_enchantments) {
			Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(pair.getLeft()));
			if (optionalEnchantmentReference.isPresent()) {
				this.prefix_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
			}
		}

		for (MutablePair<String, Integer> pair : suffix_enchantments) {
			Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(pair.getLeft()));
			if (optionalEnchantmentReference.isPresent()) {
				this.suffix_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
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

			ItemStack itemStack = inventory.getStack(0);
			ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.ENCHANTMENTS);
			if (!itemStack.isEmpty() && itemStack.getItem().isEnchantable(itemStack) && itemEnchantmentsComponent != null) {
				if (!itemEnchantmentsComponent.isEmpty()) {
					for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
						if (entry.getKey().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
							if (optionalEnchantmentReference.isPresent()) {
								this.existing_prefix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
								this.existing_enchantment_costs[0] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getMinPower(entry.getIntValue()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
								this.existing_enchantment_costs[1] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getAnvilCost() * entry.getIntValue() * serverConfig.old_enchantment_item_cost_multiplier.get()));
							}
						}
						if (entry.getKey().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
							if (optionalEnchantmentReference.isPresent()) {
								this.existing_suffix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
								this.existing_enchantment_costs[2] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getMinPower(entry.getIntValue()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
								this.existing_enchantment_costs[3] = (int) Math.max(0, Math.floor(optionalEnchantmentReference.get().value().getAnvilCost() * entry.getIntValue()  * serverConfig.old_enchantment_item_cost_multiplier.get()));
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

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		ItemStack itemStack = this.inventory.getStack(0);
		ItemStack itemStack2 = this.inventory.getStack(1);
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		if (itemStack.isEmpty()) {
			return false;
		}
		int first_threshold = this.current_prefix_enchantments.size();
		if (id >= 0 && id < first_threshold) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_prefix_enchantments.get(id);

			int experience_cost_amount = this.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getMaxPower(newEnchantment.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
			if (player.experienceLevel < experience_cost_amount && !player.isInCreativeMode()) {
				return false;
			}
			int item_cost_amount = this.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getAnvilCost() * newEnchantment.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
			if ((!itemStack2.isIn(RPGEnchanting.ENCHANTING_PREFIX_COST_ITEMS) || itemStack2.getCount() < item_cost_amount) && !player.isInCreativeMode()) {
				return false;
			}
			player.applyEnchantmentCosts(itemStack, experience_cost_amount);
			ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
			if (this.existing_prefix_enchantment != null) {
				itemEnchantmentsComponentBuilder.set(this.existing_prefix_enchantment.getLeft(), 0);
			}
			itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
			itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
			itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);

			itemStack2.decrementUnlessCreative(item_cost_amount, player);
			if (itemStack2.isEmpty()) {
				this.inventory.setStack(1, ItemStack.EMPTY);
			}

			player.incrementStat(Stats.ENCHANT_ITEM);
			if (player instanceof ServerPlayerEntity) {
				Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack, experience_cost_amount);
			}

			this.inventory.markDirty();
			this.onContentChanged(this.inventory);

			world.playSound(null, this.blockPos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);

		} else if (id >= first_threshold && id < first_threshold + this.current_suffix_enchantments.size()) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_suffix_enchantments.get(id - first_threshold);

			int experience_cost_amount = this.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getMaxPower(newEnchantment.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
			if (player.experienceLevel < experience_cost_amount && !player.isInCreativeMode()) {
				return false;
			}
			int item_cost_amount = this.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(newEnchantment.getLeft().value().getAnvilCost() * newEnchantment.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
			if ((!itemStack2.isIn(RPGEnchanting.ENCHANTING_PREFIX_COST_ITEMS) || itemStack2.getCount() < item_cost_amount) && !player.isInCreativeMode()) {
				return false;
			}
			player.applyEnchantmentCosts(itemStack, experience_cost_amount);

			ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
			if (this.existing_suffix_enchantment != null) {
				itemEnchantmentsComponentBuilder.set(this.existing_suffix_enchantment.getLeft(), 0);
			}
			itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
			itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
			itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);

			itemStack2.decrementUnlessCreative(item_cost_amount, player);
			if (itemStack2.isEmpty()) {
				this.inventory.setStack(1, ItemStack.EMPTY);
			}

			player.incrementStat(Stats.ENCHANT_ITEM);
			if (player instanceof ServerPlayerEntity) {
				Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack, experience_cost_amount);
			}

			this.inventory.markDirty();
			this.onContentChanged(this.inventory);

			world.playSound(null, this.blockPos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);

		} else {
			Util.error(player.getName() + " pressed invalid button id: " + id);
			return false;
		}
		return true;
	}

	public int getPrefixItemCount() {
		ItemStack itemStack = this.inventory.getStack(1);
		return itemStack.isEmpty() || !itemStack.isIn(RPGEnchanting.ENCHANTING_PREFIX_COST_ITEMS) ? 0 : itemStack.getCount();
	}

	public int getSuffixItemCount() {
		ItemStack itemStack = this.inventory.getStack(1);
		return itemStack.isEmpty() || !itemStack.isIn(RPGEnchanting.ENCHANTING_SUFFIX_COST_ITEMS) ? 0 : itemStack.getCount();
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
			} else if (itemStack2.isOf(Items.LAPIS_LAZULI)) {
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
			Set<MutablePair<String, Integer>> prefix_enchantments,
			Set<MutablePair<String, Integer>> suffix_enchantments
	) {

		public static final PacketCodec<RegistryByteBuf, RPGEnchanterBlockData> PACKET_CODEC = PacketCodec.of(RPGEnchanterBlockData::write, RPGEnchanterBlockData::new);

		public RPGEnchanterBlockData(RegistryByteBuf registryByteBuf) {
			this(
					registryByteBuf.readBlockPos(),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER)
			);
		}

		private void write(RegistryByteBuf registryByteBuf) {
			registryByteBuf.writeBlockPos(blockPos);
			registryByteBuf.writeCollection(this.prefix_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
			registryByteBuf.writeCollection(this.suffix_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
		}
	}

}
