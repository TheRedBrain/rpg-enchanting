package com.github.theredbrain.rpgenchanting.block.entitiy;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantingTableBlockEntity extends BlockEntity implements Nameable {
	private static final Component CONTAINER_NAME_TEXT = Component.translatable("gui.rpg_enchanting_table.title");
	public int ticks;
	public float nextPageAngle;
	public float pageAngle;
	public float flipRandom;
	public float flipTurn;
	public float nextPageTurningSpeed;
	public float pageTurningSpeed;
	public float bookRotation;
	public float lastBookRotation;
	public float targetBookRotation;
	private static final RandomSource RANDOM = RandomSource.create();
	@Nullable
	private RPGEnchantingTableBlock.BookCost customBookCost;
	@Nullable
	private RPGEnchantingTableBlock.EnchantmentUnlockMode customEnchantmentUnlockMode;
	@Nullable
	private Component customName;
	private int customBlockReachRadius = -1;

	public RPGEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
		super(EntityRegistry.RPG_ENCHANTING_TABLE, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);
		view.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
		if (this.customBookCost != null) {
			view.putString("custom_book_cost", this.customBookCost.getSerializedName());
		}
		if (this.customEnchantmentUnlockMode != null) {
			view.putString("custom_enchanting_mode", this.customEnchantmentUnlockMode.getSerializedName());
		}
		if (this.customBlockReachRadius >= 0) {
			view.putInt("custom_block_reach_radius", this.customBlockReachRadius);
		}
	}

	@Override
	protected void loadAdditional(ValueInput view) {
		super.loadAdditional(view);
		this.customName = parseCustomNameSafe(view, "CustomName");

		Optional<RPGEnchantingTableBlock.BookCost> optionalBookCost = RPGEnchantingTableBlock.BookCost.byName(view.getStringOr("custom_book_cost", ""));
		this.customBookCost = optionalBookCost.orElse(null);

		Optional<RPGEnchantingTableBlock.EnchantmentUnlockMode> optionalEnchantmentUnlockMode = RPGEnchantingTableBlock.EnchantmentUnlockMode.byName(view.getStringOr("custom_enchanting_mode", ""));
		this.customEnchantmentUnlockMode = optionalEnchantmentUnlockMode.orElse(null);

		this.customBlockReachRadius = view.getIntOr("custom_block_reach_radius", -1);
	}

	public HashSet<MutablePair<String, Integer>> getAdvancementEnchantments(Player playerEntity) {
		HashSet<MutablePair<String, Integer>> advancement_enchantments = new HashSet<>();

		if (this.level != null) {

			MinecraftServer server = level.getServer();
			ValidatedList<ServerConfig.UnlockedEnchantment> enchantments_unlocked_by_advancements = RPGEnchanting.SERVER_CONFIG.enchantments_unlocked_by_advancements;

			if (enchantments_unlocked_by_advancements.isEmpty()) {
				return advancement_enchantments;
			}

			if (playerEntity instanceof ServerPlayer serverPlayerEntity && server != null) {
				for (ServerConfig.UnlockedEnchantment unlockedEnchantment : enchantments_unlocked_by_advancements) {
					AdvancementNode placedAdvancement = server.getAdvancements().tree().get(Identifier.parse(unlockedEnchantment.identifier));
					if (placedAdvancement != null) {
						AdvancementHolder advancementEntry = placedAdvancement.holder();
						if (serverPlayerEntity.getAdvancements().getOrStartProgress(advancementEntry).isDone()) {
							Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.level.registryAccess().lookup(Registries.ENCHANTMENT);
							if (optionalEnchantmentRegistry.isPresent()) {
								Optional<Holder.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().get(Identifier.parse(unlockedEnchantment.enchantment));

								if (optionalEnchantmentReference.isPresent()) {
									advancement_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get().getRegisteredName(), unlockedEnchantment.level));
								}
							}
						}
					}
				}
			}
		}
		return advancement_enchantments;
	}

	public HashSet<MutablePair<String, Integer>> getBlockEnchantments() {
		HashSet<MutablePair<String, Integer>> block_enchantments = new HashSet<>();
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		ValidatedList<ServerConfig.UnlockedEnchantment> enchantments_unlocked_by_blocks = serverConfig.enchantments_unlocked_by_blocks;

		if (enchantments_unlocked_by_blocks.isEmpty()) {
			return block_enchantments;
		}
		int rpg_enchanting_table_block_reach_radius = this.getBlockReachRadius();
		Map<Block, HashSet<MutablePair<Holder.Reference<Enchantment>, Integer>>> blockMap = new HashMap<>();
		for (ServerConfig.UnlockedEnchantment unlockedEnchantment : enchantments_unlocked_by_blocks) {
			Holder.Reference<Block> registryBlockEntry = null;
			Optional<Holder.Reference<Block>> optionalBlock = BuiltInRegistries.BLOCK.get(Identifier.parse(unlockedEnchantment.identifier));

			if (optionalBlock.isPresent()) {
				registryBlockEntry = optionalBlock.get();
			}
			if (this.level != null) {
				Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.level.registryAccess().lookup(Registries.ENCHANTMENT);
				if (optionalEnchantmentRegistry.isPresent()) {
					Optional<Holder.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().get(Identifier.parse(unlockedEnchantment.enchantment));

					if (registryBlockEntry != null && optionalEnchantmentReference.isPresent()) {
						HashSet<MutablePair<Holder.Reference<Enchantment>, Integer>> arrayList = blockMap.getOrDefault(registryBlockEntry.value(), new HashSet<>());
						arrayList.add(new MutablePair<>(optionalEnchantmentReference.get(), unlockedEnchantment.level));
						blockMap.put(registryBlockEntry.value(), arrayList);
					}
				}
			}
		}
		if (blockMap.isEmpty()) {
			return block_enchantments;
		}
		BlockState blockState;
		int posX = this.worldPosition.getX();
		int posY = this.worldPosition.getY();
		int posZ = this.worldPosition.getZ();
		for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
			for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
				for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
					BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
					blockState = level.getBlockState(blockPos);
					Block block = blockState.getBlock();
					Set<MutablePair<Holder.Reference<Enchantment>, Integer>> set = blockMap.get(block);

					if (set != null) {
						for (MutablePair<Holder.Reference<Enchantment>, Integer> pair : set) {
							block_enchantments.add(new MutablePair<>(pair.getLeft().getRegisteredName(), pair.getRight()));
						}
						blockMap.remove(block);
					}
					if (blockMap.isEmpty()) {
						break;
					}
				}
				if (blockMap.isEmpty()) {
					break;
				}
			}
			if (blockMap.isEmpty()) {
				break;
			}
		}
		return block_enchantments;
	}

	public HashSet<MutablePair<String, Integer>> getBookEnchantments() {
		HashSet<MutablePair<String, Integer>> book_enchantments = new HashSet<>();
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		if (!serverConfig.enable_enchantment_unlocking_by_chiseled_bookshelves.get()) {
			return book_enchantments;
		}
		int posX = worldPosition.getX();
		int posY = worldPosition.getY();
		int posZ = worldPosition.getZ();
		int rpg_enchanting_table_block_reach_radius = this.getBlockReachRadius();
		BlockState blockState;

		for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
			for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
				for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
					BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
					blockState = level.getBlockState(blockPos);

					if (blockState.getBlock() instanceof ChiseledBookShelfBlock) {
						BlockEntity blockEntity = level.getBlockEntity(blockPos);

						if (blockEntity instanceof ChiseledBookShelfBlockEntity chiseledBookshelfBlockEntity) {
							for (int l = 0; l < 6; l++) {
								ItemStack itemStack = chiseledBookshelfBlockEntity.getItem(l);
								ItemEnchantments itemEnchantmentsComponent = itemStack.get(DataComponents.STORED_ENCHANTMENTS);

								if (itemEnchantmentsComponent != null) {
									for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantmentsComponent.entrySet()) {
										book_enchantments.add(new MutablePair<>(entry.getKey().getRegisteredName(), entry.getIntValue()));
									}
								}
							}
						}
					}
				}
			}
		}
		return book_enchantments;
	}

	public RPGEnchantingTableBlock.BookCost getBookCost() {
		return this.customBookCost != null ? this.customBookCost : RPGEnchanting.SERVER_CONFIG.default_book_cost.get();
	}

	public RPGEnchantingTableBlock.EnchantmentUnlockMode getEnchantingMode() {
		return this.customEnchantmentUnlockMode != null ? this.customEnchantmentUnlockMode : RPGEnchanting.SERVER_CONFIG.default_enchantment_unlock_mode.get();
	}

	public int getBlockReachRadius() {
		return this.customBlockReachRadius >= 0 ? this.customBlockReachRadius : RPGEnchanting.SERVER_CONFIG.rpg_enchanting_table_block_reach_radius.get();
	}

	public boolean applyBookCost(MutablePair<Holder.Reference<Enchantment>, Integer> enchantment) {

		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		int posX = this.worldPosition.getX();
		int posY = this.worldPosition.getY();
		int posZ = this.worldPosition.getZ();

		BlockState blockState;
		int rpg_enchanting_table_block_reach_radius = this.getBlockReachRadius();
		RPGEnchantingTableBlock.BookCost bookCost = this.getBookCost();

		if (bookCost == RPGEnchantingTableBlock.BookCost.KEEP) {
			return true;
		}

		if (this.level != null) {
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
						blockState = this.level.getBlockState(blockPos);

						if (blockState.getBlock() instanceof ChiseledBookShelfBlock) {
							BlockEntity blockEntity = this.level.getBlockEntity(blockPos);

							if (blockEntity instanceof ChiseledBookShelfBlockEntity chiseledBookshelfBlockEntity) {
								for (int l = 0; l < 6; l++) {
									ItemStack itemStack = chiseledBookshelfBlockEntity.getItem(l);
									ItemEnchantments itemEnchantmentsComponent = itemStack.get(DataComponents.STORED_ENCHANTMENTS);
									boolean replaceBook = false;

									if (itemEnchantmentsComponent != null) {
										for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantmentsComponent.entrySet()) {

											if (entry.getKey() == enchantment.getLeft() && entry.getIntValue() == enchantment.getRight()) {
												replaceBook = true;
											}
										}
									}

									if (replaceBook) {
										ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(itemEnchantmentsComponent);
										builder.set(enchantment.getLeft(), 0);
										ItemStack newStack;

										if (bookCost == RPGEnchantingTableBlock.BookCost.CONSUME || builder.keySet().isEmpty()) {
											newStack = new ItemStack(BuiltInRegistries.ITEM.getValue(serverConfig.enchanted_book_replacement.get()));
										} else {
											newStack = itemStack.copy();
											newStack.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
										}
										chiseledBookshelfBlockEntity.setItem(l, newStack);
										chiseledBookshelfBlockEntity.setChanged();
										this.level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL);
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public static void tick(Level world, BlockPos pos, BlockState state, RPGEnchantingTableBlockEntity blockEntity) {
		blockEntity.pageTurningSpeed = blockEntity.nextPageTurningSpeed;
		blockEntity.lastBookRotation = blockEntity.bookRotation;
		Player playerEntity = world.getNearestPlayer((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, 3.0, false);
		if (playerEntity != null) {
			double d = playerEntity.getX() - ((double) pos.getX() + 0.5);
			double e = playerEntity.getZ() - ((double) pos.getZ() + 0.5);
			blockEntity.targetBookRotation = (float) Mth.atan2(e, d);
			blockEntity.nextPageTurningSpeed += 0.1F;
			if (blockEntity.nextPageTurningSpeed < 0.5F || RANDOM.nextInt(40) == 0) {
				float f = blockEntity.flipRandom;

				do {
					blockEntity.flipRandom += (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
				} while (f == blockEntity.flipRandom);
			}
		} else {
			blockEntity.targetBookRotation += 0.02F;
			blockEntity.nextPageTurningSpeed -= 0.1F;
		}

		while (blockEntity.bookRotation >= 3.1415927F) {
			blockEntity.bookRotation -= 6.2831855F;
		}

		while (blockEntity.bookRotation < -3.1415927F) {
			blockEntity.bookRotation += 6.2831855F;
		}

		while (blockEntity.targetBookRotation >= 3.1415927F) {
			blockEntity.targetBookRotation -= 6.2831855F;
		}

		while (blockEntity.targetBookRotation < -3.1415927F) {
			blockEntity.targetBookRotation += 6.2831855F;
		}

		float g;
		for (g = blockEntity.targetBookRotation - blockEntity.bookRotation; g >= 3.1415927F; g -= 6.2831855F) {
		}

		while (g < -3.1415927F) {
			g += 6.2831855F;
		}

		blockEntity.bookRotation += g * 0.4F;
		blockEntity.nextPageTurningSpeed = Mth.clamp(blockEntity.nextPageTurningSpeed, 0.0F, 1.0F);
		++blockEntity.ticks;
		blockEntity.pageAngle = blockEntity.nextPageAngle;
		float h = (blockEntity.flipRandom - blockEntity.nextPageAngle) * 0.4F;
		float i = 0.2F;
		h = Mth.clamp(h, -0.2F, 0.2F);
		blockEntity.flipTurn += (h - blockEntity.flipTurn) * 0.9F;
		blockEntity.nextPageAngle += blockEntity.flipTurn;
	}

	@Override
	public Component getName() {
		return (Component) (this.customName != null ? this.customName : CONTAINER_NAME_TEXT);
	}

	public void setCustomName(@Nullable Component customName) {
		this.customName = customName;
	}

	@Nullable
	@Override
	public Component getCustomName() {
		return this.customName;
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		this.customName = (Component) components.get(DataComponents.CUSTOM_NAME);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
		super.collectImplicitComponents(componentMapBuilder);
		componentMapBuilder.set(DataComponents.CUSTOM_NAME, this.customName);
	}

	@Override
	public void removeComponentsFromTag(ValueOutput view) {
		view.discard("CustomName");
	}
}
