package com.github.theredbrain.rpgenchanting.screen;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.slotcustomizationapi.api.SlotCustomization;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.collection.IndexedIterable;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantmentScreenHandler extends ScreenHandler {
	private final Inventory inventory = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			RPGEnchantmentScreenHandler.this.onContentChanged(this);
		}
	};

	private final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> consumable_enchantments = new HashSet<>();
	private final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> prefix_enchantments = new HashSet<>();
	private final Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> suffix_enchantments = new HashSet<>();
	private final List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = new ArrayList<>();
	private final List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = new ArrayList<>();
	private MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = null;
	private MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = null;

	public final int[] currentPrefixEnchantmentIds = new int[]{-1, -1, -1, -1};
	public final int[] currentSuffixEnchantmentIds = new int[]{-1, -1, -1, -1};
	public final int[] currentPrefixEnchantmentLevels = new int[]{-1, -1, -1, -1};
	public final int[] currentSuffixEnchantmentLevels = new int[]{-1, -1, -1, -1};
	public final int[] listInformation = new int[]{0, 0, 0, 0};
	public final int[] existing_enchantment_costs = new int[]{0, 0, 0, 0};
	public final int[] existingEnchantments = new int[]{-1, -1, -1, -1};
	public final int[] consumePrefixEnchantments = new int[]{0, 0, 0, 0};
	public final int[] consumeSuffixEnchantments = new int[]{0, 0, 0, 0};
	public final int[] newPrefixExpCosts = new int[]{0, 0, 0, 0};
	public final int[] newSuffixExpCosts = new int[]{0, 0, 0, 0};
	public final int[] newPrefixItemCosts = new int[]{0, 0, 0, 0};
	public final int[] newSuffixItemCosts = new int[]{0, 0, 0, 0};

	private final ScreenHandlerContext context;
	public final PlayerEntity player;

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public RPGEnchantmentScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context/*, BlockPos blockPos, Set<MutablePair<String, Integer>> advancement_enchantments, Set<MutablePair<String, Integer>> block_enchantments, Set<MutablePair<String, Integer>> book_enchantments*/) {
		super(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, syncId);
		this.context = context;
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
		// 0 - 3
		this.addProperty(Property.create(this.currentPrefixEnchantmentIds, 0));
		this.addProperty(Property.create(this.currentPrefixEnchantmentIds, 1));
		this.addProperty(Property.create(this.currentPrefixEnchantmentIds, 2));
		this.addProperty(Property.create(this.currentPrefixEnchantmentIds, 3));

		// 4 - 7
		this.addProperty(Property.create(this.currentPrefixEnchantmentLevels, 0));
		this.addProperty(Property.create(this.currentPrefixEnchantmentLevels, 1));
		this.addProperty(Property.create(this.currentPrefixEnchantmentLevels, 2));
		this.addProperty(Property.create(this.currentPrefixEnchantmentLevels, 3));

		// 8 - 11
		this.addProperty(Property.create(this.currentSuffixEnchantmentIds, 0));
		this.addProperty(Property.create(this.currentSuffixEnchantmentIds, 1));
		this.addProperty(Property.create(this.currentSuffixEnchantmentIds, 2));
		this.addProperty(Property.create(this.currentSuffixEnchantmentIds, 3));

		// 12 - 15
		this.addProperty(Property.create(this.currentSuffixEnchantmentLevels, 0));
		this.addProperty(Property.create(this.currentSuffixEnchantmentLevels, 1));
		this.addProperty(Property.create(this.currentSuffixEnchantmentLevels, 2));
		this.addProperty(Property.create(this.currentSuffixEnchantmentLevels, 3));

		// 16 - 19
		this.addProperty(Property.create(this.listInformation, 0)); // current_prefix_enchantments.size()
		this.addProperty(Property.create(this.listInformation, 1)); // prefixEnchantmentsScrollPosition
		this.addProperty(Property.create(this.listInformation, 2)); // current_suffix_enchantments.size()
		this.addProperty(Property.create(this.listInformation, 3)); // suffixEnchantmentsScrollPosition

		// 20 - 23
		this.addProperty(Property.create(this.existing_enchantment_costs, 0)); // prefix exp cost
		this.addProperty(Property.create(this.existing_enchantment_costs, 1)); // prefix item cost
		this.addProperty(Property.create(this.existing_enchantment_costs, 2)); // suffix exp cost
		this.addProperty(Property.create(this.existing_enchantment_costs, 3)); // suffix item cost

		// 24 - 27
		this.addProperty(Property.create(this.existingEnchantments, 0)); // existing prefix id
		this.addProperty(Property.create(this.existingEnchantments, 1)); // existing prefix level
		this.addProperty(Property.create(this.existingEnchantments, 2)); // existing suffix id
		this.addProperty(Property.create(this.existingEnchantments, 3)); // existing suffix level

		// 28 - 31
		this.addProperty(Property.create(this.consumePrefixEnchantments, 0));
		this.addProperty(Property.create(this.consumePrefixEnchantments, 1));
		this.addProperty(Property.create(this.consumePrefixEnchantments, 2));
		this.addProperty(Property.create(this.consumePrefixEnchantments, 3));

		// 32 - 35
		this.addProperty(Property.create(this.consumeSuffixEnchantments, 0));
		this.addProperty(Property.create(this.consumeSuffixEnchantments, 1));
		this.addProperty(Property.create(this.consumeSuffixEnchantments, 2));
		this.addProperty(Property.create(this.consumeSuffixEnchantments, 3));
	}

	public void updateEnchantmentLists() {
		this.context.run((world, pos) -> {

			List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> advancement_enchantments = new ArrayList<>();
			List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> block_enchantments = new ArrayList<>();
			List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> book_enchantments = new ArrayList<>();

			this.prefix_enchantments.clear();
			this.suffix_enchantments.clear();
			this.consumable_enchantments.clear();
			RPGEnchantingTableBlock.EnchantingMode enchantingMode = RPGEnchantingTableBlock.getEnchantingMode(world, pos);
			RPGEnchantingTableBlock.BookCost bookCost = RPGEnchantingTableBlock.getBookCost(world, pos);

			for (MutablePair<String, Integer> pair : RPGEnchantingTableBlock.getAdvancementEnchantments(world, this.player)) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					advancement_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}

			for (MutablePair<String, Integer> pair : RPGEnchantingTableBlock.getBlockEnchantments(world, pos)) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					block_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}

			for (MutablePair<String, Integer> pair : RPGEnchantingTableBlock.getBookshelfEnchantments(world, pos)) {
				Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(pair.getLeft()));
				if (optionalEnchantmentReference.isPresent()) {
					book_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get(), pair.getRight()));
				}
			}

			if (enchantingMode == RPGEnchantingTableBlock.EnchantingMode.BLOCK_REQUIRED_FOR_ADVANCEMENT) {
				for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : book_enchantments) {

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
				for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : advancement_enchantments) {

					if (block_enchantments.contains(pair)) {

						if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
						}
						if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
						}
						this.consumable_enchantments.remove(pair);
					}
				}
			} else if (enchantingMode == RPGEnchantingTableBlock.EnchantingMode.ADDITION) {
				for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : book_enchantments) {

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
				for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : advancement_enchantments) {

					if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
						this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
						this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					this.consumable_enchantments.remove(pair);
				}
				for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : block_enchantments) {

					if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
						this.prefix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
						this.suffix_enchantments.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
					}
					this.consumable_enchantments.remove(pair);
				}
			}
			this.sendContentUpdates();
		});
	}

	public void updateVisibleEnchantments() {
		RPGEnchanting.info("++++++++++++++++++ updateVisibleEnchantments ++++++++++++++++++");
		this.context.run((world, pos) -> {
			ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
			RPGEnchantingTableBlock.BookCost bookCost = RPGEnchantingTableBlock.getBookCost(world, pos);
			IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getIndexedEntries();

			int index = 0;
			for (int i = this.listInformation[1]; index < 4; i++) {
				if (i < this.current_prefix_enchantments.size()) {
					MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = this.current_prefix_enchantments.get(i);
					this.currentPrefixEnchantmentIds[index] = indexedIterable.getRawId(entry.getLeft());
					this.currentPrefixEnchantmentLevels[index] = entry.getRight();
					this.newPrefixExpCosts[index] = (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
					this.newPrefixItemCosts[index] = (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
					if (bookCost != RPGEnchantingTableBlock.BookCost.KEEP && this.consumable_enchantments.contains(entry)) {
						this.consumePrefixEnchantments[index] = 1;
					} else {
						this.consumePrefixEnchantments[index] = 0;
					}
					this.sendContentUpdates();
				} else {
					this.currentPrefixEnchantmentIds[index] = -1;
					this.currentPrefixEnchantmentLevels[index] = -1;
					this.consumePrefixEnchantments[index] = 0;
					this.newPrefixExpCosts[index] = 0;
					this.newPrefixItemCosts[index] = 0;
					this.sendContentUpdates();
				}
				index++;
			}
			this.listInformation[0] = this.current_prefix_enchantments.size();
			this.sendContentUpdates();

			index = 0;
			for (int i = this.listInformation[3]; index < 4; i++) {
				if (i < this.current_suffix_enchantments.size()) {
					MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = this.current_suffix_enchantments.get(i);
					this.currentSuffixEnchantmentIds[index] = indexedIterable.getRawId(entry.getLeft());
					this.currentSuffixEnchantmentLevels[index] = entry.getRight();

					RPGEnchanting.info("new suffix exp cost: " + entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get());
					this.newSuffixExpCosts[index] = (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
					this.newSuffixItemCosts[index] = (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
					if (bookCost != RPGEnchantingTableBlock.BookCost.KEEP && this.consumable_enchantments.contains(entry)) {
						this.consumeSuffixEnchantments[index] = 1;
					} else {
						this.consumeSuffixEnchantments[index] = 0;
					}
					this.sendContentUpdates();
				} else {
					this.currentSuffixEnchantmentIds[index] = -1;
					this.currentSuffixEnchantmentLevels[index] = -1;
					this.consumeSuffixEnchantments[index] = 0;
					this.newSuffixExpCosts[index] = 0;
					this.newSuffixItemCosts[index] = 0;
					this.sendContentUpdates();
				}
				index++;
			}
			this.listInformation[2] = this.current_suffix_enchantments.size();

			this.sendContentUpdates();
		});

		RPGEnchanting.info("newPrefixExpCosts: " + Arrays.toString(this.newPrefixExpCosts));
		RPGEnchanting.info("newPrefixItemCosts: " + Arrays.toString(this.newPrefixItemCosts));
		RPGEnchanting.info("newSuffixExpCosts: " + Arrays.toString(this.newSuffixExpCosts));
		RPGEnchanting.info("newSuffixItemCosts: " + Arrays.toString(this.newSuffixItemCosts));
		RPGEnchanting.info("++++++++++++++++++++++++++++++++++++");

	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);

		if (inventory == this.inventory) {

			this.updateEnchantmentLists();

			this.context.run((world, pos) -> {
				this.existing_prefix_enchantment = null;
				this.existing_suffix_enchantment = null;
				this.current_prefix_enchantments.clear();
				this.current_suffix_enchantments.clear();

				ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
				IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getIndexedEntries();
				ItemStack itemStack = inventory.getStack(0);
				ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.ENCHANTMENTS);

				if (!itemStack.isEmpty() && itemStack.getItem().isEnchantable(itemStack) && itemEnchantmentsComponent != null) {

					if (!itemEnchantmentsComponent.isEmpty()) {
						for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
							if (entry.getKey().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
								Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
								if (optionalEnchantmentReference.isPresent()) {
									this.existing_prefix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
								}
							}
							if (entry.getKey().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
								Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(entry.getKey().getIdAsString()));
								if (optionalEnchantmentReference.isPresent()) {
									this.existing_suffix_enchantment = new MutablePair<>(optionalEnchantmentReference.get(), entry.getIntValue());
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

				// update properties
				if (this.existing_prefix_enchantment != null) {
					this.existingEnchantments[0] = indexedIterable.getRawId(this.existing_prefix_enchantment.getLeft());
					this.existingEnchantments[1] = this.existing_prefix_enchantment.getRight();
					this.existing_enchantment_costs[0] = (int) Math.max(0, Math.floor(this.existing_prefix_enchantment.getLeft().value().getMinPower(this.existing_prefix_enchantment.getRight()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
					this.existing_enchantment_costs[1] = (int) Math.max(0, Math.floor(this.existing_prefix_enchantment.getLeft().value().getAnvilCost() * this.existing_prefix_enchantment.getRight() * serverConfig.old_enchantment_item_cost_multiplier.get()));
				} else {
					this.existingEnchantments[0] = -1;
					this.existingEnchantments[1] = -1;
					this.existing_enchantment_costs[0] = 0;
					this.existing_enchantment_costs[1] = 0;
				}
				if (this.existing_suffix_enchantment != null) {
					this.existingEnchantments[2] = indexedIterable.getRawId(this.existing_suffix_enchantment.getLeft());
					this.existingEnchantments[3] = this.existing_suffix_enchantment.getRight();
					this.existing_enchantment_costs[2] = (int) Math.max(0, Math.floor(this.existing_suffix_enchantment.getLeft().value().getMinPower(this.existing_suffix_enchantment.getRight()) * serverConfig.old_enchantment_exp_cost_multiplier.get()));
					this.existing_enchantment_costs[3] = (int) Math.max(0, Math.floor(this.existing_suffix_enchantment.getLeft().value().getAnvilCost() * this.existing_suffix_enchantment.getRight() * serverConfig.old_enchantment_item_cost_multiplier.get()));
				} else {
					this.existingEnchantments[2] = -1;
					this.existingEnchantments[3] = -1;
					this.existing_enchantment_costs[2] = 0;
					this.existing_enchantment_costs[3] = 0;
				}

//				RPGEnchanting.info("onContentChanged");
//				RPGEnchanting.info("existing_enchantment_costs: " + Arrays.toString(this.existing_enchantment_costs));

				this.sendContentUpdates();
			});

			this.updateVisibleEnchantments();
		}
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {

		ItemStack itemStack = this.inventory.getStack(0);
		ItemStack itemStack2 = this.inventory.getStack(1);
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		if (itemStack.isEmpty()) {
			RPGEnchanting.info("itemStack empty");
			return false;
		}

		if (id >= 0 && id < 4) {
//			RPGEnchanting.info("existing_enchantment_costs: " + Arrays.toString(this.existing_enchantment_costs));
//			RPGEnchanting.info("###########");
//			RPGEnchanting.info("newPrefixExpCosts: " + this.newPrefixExpCosts[id]);
//			RPGEnchanting.info("exp cost: " + (this.existing_enchantment_costs[0] + this.newPrefixExpCosts[id]));
//			RPGEnchanting.info("###########");
//			RPGEnchanting.info("newPrefixItemCosts: " + this.newPrefixItemCosts[id]);
//			RPGEnchanting.info("item cost: " + (this.existing_enchantment_costs[1] + this.newPrefixItemCosts[id]));

			int experience_cost_amount = this.existing_enchantment_costs[0] + this.newPrefixExpCosts[id];
			if (player.experienceLevel < experience_cost_amount && !player.isInCreativeMode()) {
				RPGEnchanting.info("exp too low");
				return false;
			} else if ((!itemStack2.isOf(Registries.ITEM.get(serverConfig.prefix_item_cost.get())) || (itemStack2.getCount() < (this.existing_enchantment_costs[1] + this.newPrefixItemCosts[id]))) && !player.isInCreativeMode()) {
				RPGEnchanting.info("not enough items");
				return false;
			} else {
				this.context.run((world, pos) -> {
					Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(this.currentPrefixEnchantmentIds[id]);

					if (optionalEnchantmentReference.isPresent()) {
						MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = new MutablePair<>(optionalEnchantmentReference.get(), this.currentPrefixEnchantmentLevels[id]);

						if (!this.consumable_enchantments.contains(newEnchantment) || RPGEnchantingTableBlock.applyBookCost(newEnchantment, world, pos)) {
							player.applyEnchantmentCosts(itemStack, experience_cost_amount);
							ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
							if (this.existing_prefix_enchantment != null) {
								itemEnchantmentsComponentBuilder.set(this.existing_prefix_enchantment.getLeft(), 0);
							}
							itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
							itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
							itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
							if (serverConfig.enable_enchanted_by_player_component_application.get()) {
								itemStack.set(RPGEnchanting.PLAYER_ENCHANTED, new ProfileComponent(player.getGameProfile()));
							}

							itemStack2.decrementUnlessCreative(this.existing_enchantment_costs[1] + this.newPrefixItemCosts[id], player);
							if (itemStack2.isEmpty()) {
								this.inventory.setStack(1, ItemStack.EMPTY);
							}

							player.incrementStat(Stats.ENCHANT_ITEM);
							if (player instanceof ServerPlayerEntity) {
								Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack, experience_cost_amount);
							}

							world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
						} else {
							RPGEnchanting.info("book couldn't be consumed");
						}
					}
					this.inventory.markDirty();
					this.sendContentUpdates();
					this.onContentChanged(this.inventory);
				});
				return true;
			}
		} else if (id >= 4 && id < 8) {
//			RPGEnchanting.info("existing_enchantment_costs: " + Arrays.toString(this.existing_enchantment_costs));
//			RPGEnchanting.info("###########");
//			RPGEnchanting.info("newSuffixExpCosts: " + this.newSuffixExpCosts[id - 4]);
//			RPGEnchanting.info("exp cost: " + (this.existing_enchantment_costs[2] + this.newSuffixExpCosts[id - 4]));
//			RPGEnchanting.info("###########");
//			RPGEnchanting.info("newSuffixItemCosts: " + this.newSuffixItemCosts[id - 4]);
//			RPGEnchanting.info("item cost: " + (this.existing_enchantment_costs[3] + this.newSuffixItemCosts[id - 4]));

			int experience_cost_amount = this.existing_enchantment_costs[2] + this.newSuffixExpCosts[id - 4];
			if (player.experienceLevel < experience_cost_amount && !player.isInCreativeMode()) {
				RPGEnchanting.info("exp too low");
				return false;
			} else if ((!itemStack2.isOf(Registries.ITEM.get(serverConfig.suffix_item_cost.get())) || itemStack2.getCount() < (this.existing_enchantment_costs[3] + this.newSuffixItemCosts[id - 4])) && !player.isInCreativeMode()) {
				RPGEnchanting.info("not enough items");
				return false;
			} else {
				this.context.run((world, pos) -> {
					Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(this.currentSuffixEnchantmentIds[id - 4]);

					if (optionalEnchantmentReference.isPresent()) {
						MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = new MutablePair<>(optionalEnchantmentReference.get(), this.currentSuffixEnchantmentLevels[id - 4]);

						if (!this.consumable_enchantments.contains(newEnchantment) || RPGEnchantingTableBlock.applyBookCost(newEnchantment, world, pos)) {
							player.applyEnchantmentCosts(itemStack, experience_cost_amount);
							ItemEnchantmentsComponent.Builder itemEnchantmentsComponentBuilder = new ItemEnchantmentsComponent.Builder(itemStack.getEnchantments());
							if (this.existing_suffix_enchantment != null) {
								itemEnchantmentsComponentBuilder.set(this.existing_suffix_enchantment.getLeft(), 0);
							}
							itemEnchantmentsComponentBuilder.add(newEnchantment.getLeft(), newEnchantment.getRight());
							itemStack.set(DataComponentTypes.ENCHANTMENTS, itemEnchantmentsComponentBuilder.build().withShowInTooltip(false));
							itemStack.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
							if (serverConfig.enable_enchanted_by_player_component_application.get()) {
								itemStack.set(RPGEnchanting.PLAYER_ENCHANTED, new ProfileComponent(player.getGameProfile()));
							}

							itemStack2.decrementUnlessCreative(this.existing_enchantment_costs[3] + this.newSuffixItemCosts[id - 4], player);
							if (itemStack2.isEmpty()) {
								this.inventory.setStack(1, ItemStack.EMPTY);
							}

							player.incrementStat(Stats.ENCHANT_ITEM);
							if (player instanceof ServerPlayerEntity) {
								Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack, experience_cost_amount);
							}

							world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
						} else {
							RPGEnchanting.info("book couldn't be consumed");
						}
					}
					this.inventory.markDirty();
					this.sendContentUpdates();
					this.onContentChanged(this.inventory);
				});
				return true;
			}
		} else {
			Util.error(player.getName() + " pressed invalid button id: " + id);
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
}
