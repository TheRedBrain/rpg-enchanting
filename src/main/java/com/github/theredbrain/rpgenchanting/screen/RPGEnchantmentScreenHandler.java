package com.github.theredbrain.rpgenchanting.screen;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.registry.ItemComponentRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.slotcustomizationapi.api.SlotCustomization;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
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
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
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
	//	private final ScreenHandlerContext context;
//	private final Random random = Random.create();
//	private final Property seed = Property.create();
	//	public final int[] enchantmentPower = new int[3];
//	public final int[] enchantmentId = new int[]{-1, -1, -1};
//	public final int[] enchantmentLevel = new int[]{-1, -1, -1};
//	public final Property currentPage = Property.create();
//	public final Property prefixAmount = Property.create();
//	public final Property suffixAmount = Property.create();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> prefix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> suffix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = new ArrayList<>();
	public List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = new ArrayList<>();
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = null;
	public MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = null;
	private final World world;

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, RPGEnchanterBlockData data) {
		this(syncId, playerInventory, data.prefix_enchantments, data.suffix_enchantments);
	}

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, Set<MutablePair<String, Integer>> prefix_enchantments, Set<MutablePair<String, Integer>> suffix_enchantments) {
		super(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, syncId);
		this.world = playerInventory.player.getWorld();
		this.addSlot(new Slot(this.inventory, 0, 134, 40) {
			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		this.addSlot(new Slot(this.inventory, 1, 134, 62) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isIn(RPGEnchanting.ENCHANTING_COST_ITEMS);
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

		((SlotCustomization) this.slots.get(1)).slotcustomizationapi$setDisabledOverride(RPGEnchanting.SERVER_CONFIG.enable_item_cost.get());

		// Inventory Size Attributes compatibility
		int activeHotbarSize = RPGEnchanting.getActiveHotbarSize(playerInventory.player);
		int activeInventorySize = RPGEnchanting.getActiveInventorySize(playerInventory.player);
		for (i = 0; i < 9; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= activeHotbarSize);
		}
		for (i = 9; i < 36; i++) {
			((SlotCustomization) this.slots.get(i)).slotcustomizationapi$setDisabledOverride(i >= 9 + activeInventorySize);
		}

//		this.addProperty(Property.create(this.enchantmentPower, 0));
//		this.addProperty(Property.create(this.enchantmentPower, 1));
//		this.addProperty(Property.create(this.enchantmentPower, 2));
//		this.addProperty(this.seed).set(playerInventory.player.getEnchantmentTableSeed());
//		this.addProperty(Property.create(this.enchantmentId, 0));
//		this.addProperty(Property.create(this.enchantmentId, 1));
//		this.addProperty(Property.create(this.enchantmentId, 2));
//		this.addProperty(Property.create(this.enchantmentLevel, 0));
//		this.addProperty(Property.create(this.enchantmentLevel, 1));
//		this.addProperty(Property.create(this.enchantmentLevel, 2));
//		this.addProperty(this.currentPage).set(0);
//		this.addProperty(this.prefixAmount).set(0);
//		this.addProperty(this.suffixAmount).set(0);

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
//			this.currentPage.set(0);

			ItemStack itemStack = inventory.getStack(0);
			ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.ENCHANTMENTS);
			if (!itemStack.isEmpty() && itemStack.getItem().isEnchantable(itemStack) && itemEnchantmentsComponent != null) {
				if (!itemEnchantmentsComponent.isEmpty()) {
					for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
						if (entry.getKey().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
							if (optionalEnchantmentReference.isPresent()) {
								this.existing_prefix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
							}
						}
						if (entry.getKey().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = this.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
							if (optionalEnchantmentReference.isPresent()) {
								this.existing_suffix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
							}
						}
					}
				}
				RPGEnchanting.LOGGER.info("existing_prefix_enchantment: " + this.existing_prefix_enchantment);
				RPGEnchanting.LOGGER.info("existing_suffix_enchantment: " + this.existing_suffix_enchantment);

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
				// TODO check itemStack for existing enchants
				//  populate prefix/suffix lists
				//		check if itemStack can be enchanted
				//		remove existing enchants
//				this.context.run((world, pos) -> {
//					IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getIndexedEntries();
//					int ix = 0;
//
//					for (BlockPos blockPos : RPGEnchantingTableBlock.POWER_PROVIDER_OFFSETS) {
//						if (RPGEnchantingTableBlock.canAccessPowerProvider(world, pos, blockPos)) {
//							ix++;
//						}
//					}
//
//					this.random.setSeed(this.seed.get());
//
//					for (int j = 0; j < 3; j++) {
//						this.enchantmentPower[j] = EnchantmentHelper.calculateRequiredExperienceLevel(this.random, j, ix, itemStack);
//						this.enchantmentId[j] = -1;
//						this.enchantmentLevel[j] = -1;
//						if (this.enchantmentPower[j] < j + 1) {
//							this.enchantmentPower[j] = 0;
//						}
//					}
//
//					for (int jx = 0; jx < 3; jx++) {
//						if (this.enchantmentPower[jx] > 0) {
//							List<EnchantmentLevelEntry> list = this.generateEnchantments(world.getRegistryManager(), itemStack, jx, this.enchantmentPower[jx]);
//							if (list != null && !list.isEmpty()) {
//								EnchantmentLevelEntry enchantmentLevelEntry = (EnchantmentLevelEntry) list.get(this.random.nextInt(list.size()));
//								this.enchantmentId[jx] = indexedIterable.getRawId(enchantmentLevelEntry.enchantment);
//								this.enchantmentLevel[jx] = enchantmentLevelEntry.level;
//							}
//						}
//					}
//
//					this.sendContentUpdates();
//				});
//			} else {
//				for (int i = 0; i < 3; i++) {
//					this.enchantmentPower[i] = 0;
//					this.enchantmentId[i] = -1;
//					this.enchantmentLevel[i] = -1;
//				}
			}
		}
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		ItemStack itemStack = this.inventory.getStack(0);
		ItemStack itemStack2 = this.inventory.getStack(1);
		if (itemStack.isEmpty()) {
			return false;
		}
//		if (this.currentPage.get() == 0 && !itemStack.isEmpty()) {
//			this.currentPage.set(id == 1 ? 1 : id == 2 ? 2 : 0);
//		} else if (this.currentPage.get() == 1) {
		int first_threshold = this.current_prefix_enchantments.size();
		if (id >= 0 && id < first_threshold) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_prefix_enchantments.get(id);
//				int i = 1; // TODO get item cost count, enchantment cost * multiplier + prev_enchantment cost * multiplier + additional_cost
//				if (RPGEnchanting.SERVER_CONFIG.combine_advancement_provided_levels.get() && itemStack2.getCount() < i && !player.isInCreativeMode()) {
			ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
			if (this.existing_prefix_enchantment != null) {
				itemEnchantmentsComponentBuilder.set(this.existing_prefix_enchantment.getLeft(), 0);
			}
			itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
			itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
			itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
//				}
			this.inventory.markDirty();
		} else if (id >= first_threshold && id < first_threshold + this.current_suffix_enchantments.size()) {
			MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = this.current_suffix_enchantments.get(id);
//				int i = 1; // TODO get item cost count, enchantment cost * multiplier + prev_enchantment cost * multiplier + additional_cost
//				if (RPGEnchanting.SERVER_CONFIG.combine_advancement_provided_levels.get() && itemStack2.getCount() < i && !player.isInCreativeMode()) {
			ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
			if (this.existing_suffix_enchantment != null) {
				itemEnchantmentsComponentBuilder.set(this.existing_suffix_enchantment.getLeft(), 0);
			}
			itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
			itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
			itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
//				}
			this.inventory.markDirty();
//			} else if (id == -1) {
//				this.currentPage.set(0);
//			} else {
//				return false;
//			}
		} else {
			Util.error(player.getName() + " pressed invalid button id: " + id);
			return false;
		}
		return true;
//		if (id >= 0 && id < this.enchantmentPower.length) {
//			ItemStack itemStack = this.inventory.getStack(0);
//			ItemStack itemStack2 = this.inventory.getStack(1);
//			int i = id + 1;
//			if ((itemStack2.isEmpty() || itemStack2.getCount() < i) && !player.isInCreativeMode()) {
//				return false;
//			} else if (this.enchantmentPower[id] <= 0
//					|| itemStack.isEmpty()
//					|| (player.experienceLevel < i || player.experienceLevel < this.enchantmentPower[id]) && !player.getAbilities().creativeMode) {
//				return false;
//			} else {
//				this.context.run((world, pos) -> {
//					ItemStack itemStack3 = itemStack;
//					List<EnchantmentLevelEntry> list = this.generateEnchantments(world.getRegistryManager(), itemStack, id, this.enchantmentPower[id]);
//					if (!list.isEmpty()) {
//						player.applyEnchantmentCosts(itemStack, i);
//						if (itemStack.isOf(Items.BOOK)) {
//							itemStack3 = itemStack.withItem(Items.ENCHANTED_BOOK);
//							this.inventory.setStack(0, itemStack3);
//						}
//
//						for (EnchantmentLevelEntry enchantmentLevelEntry : list) {
//							itemStack3.addEnchantment(enchantmentLevelEntry.enchantment, enchantmentLevelEntry.level);
//						}
//
//						itemStack2.decrementUnlessCreative(i, player);
//						if (itemStack2.isEmpty()) {
//							this.inventory.setStack(1, ItemStack.EMPTY);
//						}
//
//						player.incrementStat(Stats.ENCHANT_ITEM);
//						if (player instanceof ServerPlayerEntity) {
//							Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack3, i);
//						}
//
//						this.inventory.markDirty();
//						this.seed.set(player.getEnchantmentTableSeed());
//						this.onContentChanged(this.inventory);
//						world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
//					}
//				});
//				return true;
//			}
//		} else {
//			Util.error(player.getName() + " pressed invalid button id: " + id);
//			return false;
//		}
	}

//	private List<EnchantmentLevelEntry> generateEnchantments(DynamicRegistryManager registryManager, ItemStack stack, int slot, int level) {
//		this.random.setSeed(this.seed.get() + slot);
//		Optional<RegistryEntryList.Named<Enchantment>> optional = registryManager.get(RegistryKeys.ENCHANTMENT).getEntryList(EnchantmentTags.IN_ENCHANTING_TABLE);
//		if (optional.isEmpty()) {
//			return List.of();
//		} else {
//			List<EnchantmentLevelEntry> list = EnchantmentHelper.generateEnchantments(this.random, stack, level, ((RegistryEntryList.Named) optional.get()).stream());
//			if (stack.isOf(Items.BOOK) && list.size() > 1) {
//				list.remove(this.random.nextInt(list.size()));
//			}
//
//			return list;
//		}
//	}

	public int getLapisCount() {
		ItemStack itemStack = this.inventory.getStack(1);
		return itemStack.isEmpty() ? 0 : itemStack.getCount();
	}

//	public int getSeed() {
//		return this.seed.get();
//	}
//
//	public int getCurrentPage() {
//		return this.currentPage.get();
//	}

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
			Set<MutablePair<String, Integer>> prefix_enchantments,
			Set<MutablePair<String, Integer>> suffix_enchantments
	) {

		public static final PacketCodec<RegistryByteBuf, RPGEnchanterBlockData> PACKET_CODEC = PacketCodec.of(RPGEnchanterBlockData::write, RPGEnchanterBlockData::new);

		public RPGEnchanterBlockData(RegistryByteBuf registryByteBuf) {
			this(
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER),
					registryByteBuf.readCollection(HashSet::new, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER)
			);
		}

		private void write(RegistryByteBuf registryByteBuf) {
			registryByteBuf.writeCollection(this.prefix_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
			registryByteBuf.writeCollection(this.suffix_enchantments, RPGEnchanting.MUTABLE_PAIR_STRING_INTEGER);
		}
	}

}
