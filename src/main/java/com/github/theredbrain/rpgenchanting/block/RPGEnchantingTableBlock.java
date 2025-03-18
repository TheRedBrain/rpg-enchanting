package com.github.theredbrain.rpgenchanting.block;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantingTableBlock extends BlockWithEntity {
	public static final MapCodec<RPGEnchantingTableBlock> CODEC = createCodec(RPGEnchantingTableBlock::new);
	protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
	public static final List<BlockPos> POWER_PROVIDER_OFFSETS = BlockPos.stream(-2, 0, -2, 2, 1, 2).filter((pos) -> {
		return Math.abs(pos.getX()) == 2 || Math.abs(pos.getZ()) == 2;
	}).map(BlockPos::toImmutable).toList();

	public MapCodec<RPGEnchantingTableBlock> getCodec() {
		return CODEC;
	}

	public RPGEnchantingTableBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	public static boolean canAccessPowerProvider(World world, BlockPos tablePos, BlockPos providerOffset) {
		return world.getBlockState(tablePos.add(providerOffset)).isIn(BlockTags.ENCHANTMENT_POWER_PROVIDER) && world.getBlockState(tablePos.add(providerOffset.getX() / 2, providerOffset.getY(), providerOffset.getZ() / 2)).isIn(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
	}

	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		Iterator var5 = POWER_PROVIDER_OFFSETS.iterator();

		while (var5.hasNext()) {
			BlockPos blockPos = (BlockPos) var5.next();
			if (random.nextInt(16) == 0 && canAccessPowerProvider(world, pos, blockPos)) {
				world.addParticle(ParticleTypes.ENCHANT, (double) pos.getX() + 0.5, (double) pos.getY() + 2.0, (double) pos.getZ() + 0.5, (double) ((float) blockPos.getX() + random.nextFloat()) - 0.5, (double) ((float) blockPos.getY() - random.nextFloat() - 1.0F), (double) ((float) blockPos.getZ() + random.nextFloat()) - 0.5);
			}
		}

	}

	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new RPGEnchantingTableBlockEntity(pos, state);
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? validateTicker(type, EntityRegistry.RPG_ENCHANTING_TABLE, RPGEnchantingTableBlockEntity::tick) : null;
	}

	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		} else {
			player.openHandledScreen(createRPGEnchanterBlockScreenHandlerFactory(state, world, pos, player));
			return ActionResult.CONSUME;
		}
	}

	public static NamedScreenHandlerFactory createRPGEnchanterBlockScreenHandlerFactory(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();

		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		int rpg_enchanting_table_block_reach_radius = serverConfig.rpg_enchanting_table_block_reach_radius.get();
		Map<Block, HashSet<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>>> blockMap = new HashMap<>();

		for (ServerConfig.UnlockedEnchantment unlockedEnchantment : serverConfig.enchantments_unlocked_by_blocks) {
			RegistryEntry.Reference<Block> registryBlockEntry = null;
			Optional<RegistryEntry.Reference<Block>> optionalBlock = Registries.BLOCK.getEntry(Identifier.of(unlockedEnchantment.identifier));
			if (optionalBlock.isPresent()) {
				registryBlockEntry = optionalBlock.get();
			}
			Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(unlockedEnchantment.enchantment));
			if (registryBlockEntry != null && optionalEnchantmentReference.isPresent()) {
				HashSet<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> arrayList = blockMap.getOrDefault(registryBlockEntry.value(), new HashSet<>());
				arrayList.add(new MutablePair<>(optionalEnchantmentReference.get(), unlockedEnchantment.level));
				blockMap.put(registryBlockEntry.value(), arrayList);
			}
		}
		HashSet<MutablePair<String, Integer>> prefix_enchantments = new HashSet<>();
		HashSet<MutablePair<String, Integer>> suffix_enchantments = new HashSet<>();
		BlockState blockState;
		boolean checkChiseledBookShelves = serverConfig.enable_enchantment_unlocking_by_chiseled_bookshelves.get();
		if (world != null) {
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(posX + i, posY + j, posZ + k);
						blockState = world.getBlockState(blockPos);

						Block block = blockState.getBlock();
						Set<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> set = blockMap.get(block);
						if (set != null) {
							for (MutablePair<RegistryEntry.Reference<Enchantment>, Integer> pair : set) {
								if (pair.getLeft().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
									prefix_enchantments.add(new MutablePair<>(pair.getLeft().getIdAsString(), pair.getRight()));
								}
								if (pair.getLeft().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
									suffix_enchantments.add(new MutablePair<>(pair.getLeft().getIdAsString(), pair.getRight()));
								}
							}
							blockMap.remove(block);
						}
						if (checkChiseledBookShelves && blockState.isOf(Blocks.CHISELED_BOOKSHELF)) {
							BlockEntity blockEntity = world.getBlockEntity(blockPos);
							if (blockEntity instanceof ChiseledBookshelfBlockEntity chiseledBookshelfBlockEntity) {
								for (int l = 0; l < 6; l++) {

									ItemStack itemStack = chiseledBookshelfBlockEntity.getStack(l);
									ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS);
									if (itemEnchantmentsComponent != null) {
										for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantmentsComponent.getEnchantmentEntries()) {
											if (entry.getKey().isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
												prefix_enchantments.add(new MutablePair<>(entry.getKey().getIdAsString(), entry.getIntValue()));
											}
											if (entry.getKey().isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
												suffix_enchantments.add(new MutablePair<>(entry.getKey().getIdAsString(), entry.getIntValue()));
											}
										}
									}
								}
							}
						}
						if (blockMap.isEmpty() && !checkChiseledBookShelves) {
							break;
						}
					}
					if (blockMap.isEmpty() && !checkChiseledBookShelves) {
						break;
					}
				}
				if (blockMap.isEmpty() && !checkChiseledBookShelves) {
					break;
				}
			}
			MinecraftServer server = world.getServer();
			if (player instanceof ServerPlayerEntity serverPlayerEntity && server != null) {
				for (ServerConfig.UnlockedEnchantment unlockedEnchantment : serverConfig.enchantments_unlocked_by_advancements) {
					PlacedAdvancement placedAdvancement = server.getAdvancementLoader().getManager().get(Identifier.of(unlockedEnchantment.identifier));
					if (placedAdvancement != null) {
						AdvancementEntry advancementEntry = placedAdvancement.getAdvancementEntry();
						if (serverPlayerEntity.getAdvancementTracker().getProgress(advancementEntry).isDone()) {
							Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantmentReference = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of(unlockedEnchantment.enchantment));
							if (optionalEnchantmentReference.isPresent()) {
								RegistryEntry.Reference<Enchantment> enchantmentReference = optionalEnchantmentReference.get();
								if (enchantmentReference.isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
									prefix_enchantments.add(new MutablePair<>(enchantmentReference.getIdAsString(), unlockedEnchantment.level));
								}
								if (enchantmentReference.isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
									suffix_enchantments.add(new MutablePair<>(enchantmentReference.getIdAsString(), unlockedEnchantment.level));
								}
							}
						}
					}
				}
			}
		}
//		RPGEnchanting.LOGGER.info("prefix_enchantments: " + prefix_enchantments);
//		RPGEnchanting.LOGGER.info("suffix_enchantments: " + suffix_enchantments);

//		if (state.isIn(Tags.PROVIDES_CRAFTING_TAB_1_LEVEL)) {
//			craftingTab1LevelProviders.add(state.getBlock().getTranslationKey());
//		}
//		if (state.isIn(Tags.PROVIDES_CRAFTING_TAB_2_LEVEL)) {
//			craftingTab2LevelProviders.add(state.getBlock().getTranslationKey());
//		}
//		if (state.isIn(Tags.PROVIDES_CRAFTING_TAB_3_LEVEL)) {
//			craftingTab3LevelProviders.add(state.getBlock().getTranslationKey());
//		}
//		if (state.isIn(Tags.PROVIDES_CRAFTING_TAB_4_LEVEL)) {
//			craftingTab4LevelProviders.add(state.getBlock().getTranslationKey());
//		}
//
//		tabLevels[0] = craftingTab1LevelProviders.size();
//		tabLevels[1] = craftingTab2LevelProviders.size();
//		tabLevels[2] = craftingTab3LevelProviders.size();
//		tabLevels[3] = craftingTab4LevelProviders.size();
//
//		tabProvidersInReach = (byte) (isStorageTabProviderInReach ? tabProvidersInReach | 1 << 0 : tabProvidersInReach & ~(1 << 0));
//		tabProvidersInReach = (byte) (isCraftingTab1ProviderInReach ? tabProvidersInReach | 1 << 1 : tabProvidersInReach & ~(1 << 1));
//		tabProvidersInReach = (byte) (isCraftingTab2ProviderInReach ? tabProvidersInReach | 1 << 2 : tabProvidersInReach & ~(1 << 2));
//		tabProvidersInReach = (byte) (isCraftingTab3ProviderInReach ? tabProvidersInReach | 1 << 3 : tabProvidersInReach & ~(1 << 3));
//		tabProvidersInReach = (byte) (isCraftingTab4ProviderInReach ? tabProvidersInReach | 1 << 4 : tabProvidersInReach & ~(1 << 4));
//
//		storageProvidersInReach = (byte) (isStorageArea0ProviderInReach ? storageProvidersInReach | 1 << 0 : storageProvidersInReach & ~(1 << 0));
//		storageProvidersInReach = (byte) (isStorageArea1ProviderInReach ? storageProvidersInReach | 1 << 1 : storageProvidersInReach & ~(1 << 1));
//		storageProvidersInReach = (byte) (isStorageArea2ProviderInReach ? storageProvidersInReach | 1 << 2 : storageProvidersInReach & ~(1 << 2));
//		storageProvidersInReach = (byte) (isStorageArea3ProviderInReach ? storageProvidersInReach | 1 << 3 : storageProvidersInReach & ~(1 << 3));
//		storageProvidersInReach = (byte) (isStorageArea4ProviderInReach ? storageProvidersInReach | 1 << 4 : storageProvidersInReach & ~(1 << 4));
//
//		byte finalTabProvidersInReach = tabProvidersInReach;
//		byte finalStorageProvidersInReach = storageProvidersInReach;
		return new ExtendedScreenHandlerFactory<>() {
			@Override
			public RPGEnchantmentScreenHandler.RPGEnchanterBlockData getScreenOpeningData(ServerPlayerEntity player) {
				return new RPGEnchantmentScreenHandler.RPGEnchanterBlockData(prefix_enchantments, suffix_enchantments);
			}

			@Override
			public Text getDisplayName() {
				return Text.translatable("gui.rpg_enchanting_table.title");
			}

			@Nullable
			@Override
			public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
				return new RPGEnchantmentScreenHandler(syncId, playerInventory, prefix_enchantments, suffix_enchantments);
			}
		};
	}

	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
