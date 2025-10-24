package com.github.theredbrain.rpgenchanting.block.entitiy;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantingTableBlockEntity extends BlockEntity implements Nameable {
	private static final Text CONTAINER_NAME_TEXT = Text.translatable("gui.rpg_enchanting_table.title");
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
	private static final Random RANDOM = Random.create();
	@Nullable
	private RPGEnchantingTableBlock.BookCost customBookCost;
	@Nullable
	private RPGEnchantingTableBlock.EnchantmentUnlockMode customEnchantmentUnlockMode;
	@Nullable
	private Text customName;
	private int customBlockReachRadius = -1;

	public RPGEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
		super(EntityRegistry.RPG_ENCHANTING_TABLE, pos, state);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putNullable("CustomName", TextCodecs.CODEC, this.customName);
		if (this.customBookCost != null) {
			view.putString("custom_book_cost", this.customBookCost.asString());
		}
		if (this.customEnchantmentUnlockMode != null) {
			view.putString("custom_enchanting_mode", this.customEnchantmentUnlockMode.asString());
		}
		if (this.customBlockReachRadius >= 0) {
			view.putInt("custom_block_reach_radius", this.customBlockReachRadius);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.customName = tryParseCustomName(view, "CustomName");

		Optional<RPGEnchantingTableBlock.BookCost> optionalBookCost = RPGEnchantingTableBlock.BookCost.byName(view.getString("custom_book_cost", ""));
		this.customBookCost = optionalBookCost.orElse(null);

		Optional<RPGEnchantingTableBlock.EnchantmentUnlockMode> optionalEnchantmentUnlockMode = RPGEnchantingTableBlock.EnchantmentUnlockMode.byName(view.getString("custom_enchanting_mode", ""));
		this.customEnchantmentUnlockMode = optionalEnchantmentUnlockMode.orElse(null);

		this.customBlockReachRadius = view.getInt("custom_block_reach_radius", -1);
	}

	public HashSet<MutablePair<String, Integer>> getAdvancementEnchantments(PlayerEntity playerEntity) {
		HashSet<MutablePair<String, Integer>> advancement_enchantments = new HashSet<>();

		if (this.world != null) {

			MinecraftServer server = world.getServer();
			ValidatedList<ServerConfig.UnlockedEnchantment> enchantments_unlocked_by_advancements = RPGEnchanting.SERVER_CONFIG.enchantments_unlocked_by_advancements;

			if (enchantments_unlocked_by_advancements.isEmpty()) {
				return advancement_enchantments;
			}

			if (playerEntity instanceof ServerPlayerEntity serverPlayerEntity && server != null) {
				for (ServerConfig.UnlockedEnchantment unlockedEnchantment : enchantments_unlocked_by_advancements) {
					PlacedAdvancement placedAdvancement = server.getAdvancementLoader().getManager().get(Identifier.of(unlockedEnchantment.identifier));
					if (placedAdvancement != null) {
						AdvancementEntry advancementEntry = placedAdvancement.getAdvancementEntry();
						if (serverPlayerEntity.getAdvancementTracker().getProgress(advancementEntry).isDone()) {
							Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
							if (optionalEnchantmentRegistry.isPresent()) {
								Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(unlockedEnchantment.enchantment));

								if (optionalEnchantmentReference.isPresent()) {
									advancement_enchantments.add(new MutablePair<>(optionalEnchantmentReference.get().getIdAsString(), unlockedEnchantment.level));
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
		Map<Block, HashSet<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>>> blockMap = new HashMap<>();
		for (ServerConfig.UnlockedEnchantment unlockedEnchantment : enchantments_unlocked_by_blocks) {
			RegistryEntry.Reference<Block> registryBlockEntry = null;
			Optional<RegistryEntry.Reference<Block>> optionalBlock = Registries.BLOCK.getEntry(Identifier.of(unlockedEnchantment.identifier));

			if (optionalBlock.isPresent()) {
				registryBlockEntry = optionalBlock.get();
			}
			if (this.world != null) {
				Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
				if (optionalEnchantmentRegistry.isPresent()) {
					Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = optionalEnchantmentRegistry.get().getEntry(Identifier.of(unlockedEnchantment.enchantment));

					if (registryBlockEntry != null && optionalEnchantmentReference.isPresent()) {
						HashSet<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> arrayList = blockMap.getOrDefault(registryBlockEntry.value(), new HashSet<>());
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
		int posX = this.pos.getX();
		int posY = this.pos.getY();
		int posZ = this.pos.getZ();
		for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
			for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
				for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
					BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
					blockState = world.getBlockState(blockPos);
					Block block = blockState.getBlock();
					Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> set = blockMap.get(block);

					if (set != null) {
						for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : set) {
							block_enchantments.add(new MutablePair<>(pair.getLeft().getIdAsString(), pair.getRight()));
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
		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();
		int rpg_enchanting_table_block_reach_radius = this.getBlockReachRadius();
		BlockState blockState;

		for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
			for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
				for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
					BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
					blockState = world.getBlockState(blockPos);

					if (blockState.getBlock() instanceof ChiseledBookshelfBlock) {
						BlockEntity blockEntity = world.getBlockEntity(blockPos);

						if (blockEntity instanceof ChiseledBookshelfBlockEntity chiseledBookshelfBlockEntity) {
							for (int l = 0; l < 6; l++) {
								ItemStack itemStack = chiseledBookshelfBlockEntity.getStack(l);
								ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS);

								if (itemEnchantmentsComponent != null) {
									for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
										book_enchantments.add(new MutablePair<>(entry.getKey().getIdAsString(), entry.getIntValue()));
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

	public boolean applyBookCost(MutablePair<RegistryEntry.Reference<Enchantment>, Integer> enchantment) {

		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		int posX = this.pos.getX();
		int posY = this.pos.getY();
		int posZ = this.pos.getZ();

		BlockState blockState;
		int rpg_enchanting_table_block_reach_radius = this.getBlockReachRadius();
		RPGEnchantingTableBlock.BookCost bookCost = this.getBookCost();

		if (bookCost == RPGEnchantingTableBlock.BookCost.KEEP) {
			return true;
		}

		if (this.world != null) {
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
						blockState = this.world.getBlockState(blockPos);

						if (blockState.getBlock() instanceof ChiseledBookshelfBlock) {
							BlockEntity blockEntity = this.world.getBlockEntity(blockPos);

							if (blockEntity instanceof ChiseledBookshelfBlockEntity chiseledBookshelfBlockEntity) {
								for (int l = 0; l < 6; l++) {
									ItemStack itemStack = chiseledBookshelfBlockEntity.getStack(l);
									ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS);
									boolean replaceBook = false;

									if (itemEnchantmentsComponent != null) {
										for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {

											if (entry.getKey() == enchantment.getLeft() && entry.getIntValue() == enchantment.getRight()) {
												replaceBook = true;
											}
										}
									}

									if (replaceBook) {
										ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(itemEnchantmentsComponent);
										builder.set(enchantment.getLeft(), 0);
										ItemStack newStack;

										if (bookCost == RPGEnchantingTableBlock.BookCost.CONSUME || builder.getEnchantments().isEmpty()) {
											newStack = new ItemStack(Registries.ITEM.get(serverConfig.enchanted_book_replacement.get()));
										} else {
											newStack = itemStack.copy();
											newStack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
										}
										chiseledBookshelfBlockEntity.setStack(l, newStack);
										chiseledBookshelfBlockEntity.markDirty();
										this.world.updateListeners(blockPos, blockState, blockState, Block.NOTIFY_ALL);
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

	public static void tick(World world, BlockPos pos, BlockState state, RPGEnchantingTableBlockEntity blockEntity) {
		blockEntity.pageTurningSpeed = blockEntity.nextPageTurningSpeed;
		blockEntity.lastBookRotation = blockEntity.bookRotation;
		PlayerEntity playerEntity = world.getClosestPlayer((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, 3.0, false);
		if (playerEntity != null) {
			double d = playerEntity.getX() - ((double) pos.getX() + 0.5);
			double e = playerEntity.getZ() - ((double) pos.getZ() + 0.5);
			blockEntity.targetBookRotation = (float) MathHelper.atan2(e, d);
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
		blockEntity.nextPageTurningSpeed = MathHelper.clamp(blockEntity.nextPageTurningSpeed, 0.0F, 1.0F);
		++blockEntity.ticks;
		blockEntity.pageAngle = blockEntity.nextPageAngle;
		float h = (blockEntity.flipRandom - blockEntity.nextPageAngle) * 0.4F;
		float i = 0.2F;
		h = MathHelper.clamp(h, -0.2F, 0.2F);
		blockEntity.flipTurn += (h - blockEntity.flipTurn) * 0.9F;
		blockEntity.nextPageAngle += blockEntity.flipTurn;
	}

	@Override
	public Text getName() {
		return (Text) (this.customName != null ? this.customName : CONTAINER_NAME_TEXT);
	}

	public void setCustomName(@Nullable Text customName) {
		this.customName = customName;
	}

	@Nullable
	@Override
	public Text getCustomName() {
		return this.customName;
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		this.customName = (Text) components.get(DataComponentTypes.CUSTOM_NAME);
	}

	@Override
	protected void addComponents(ComponentMap.Builder componentMapBuilder) {
		super.addComponents(componentMapBuilder);
		componentMapBuilder.add(DataComponentTypes.CUSTOM_NAME, this.customName);
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		view.remove("CustomName");
	}
}
