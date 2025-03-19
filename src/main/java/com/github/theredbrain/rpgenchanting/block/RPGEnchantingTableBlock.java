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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantingTableBlock extends BlockWithEntity {
	public static final MapCodec<RPGEnchantingTableBlock> CODEC = createCodec(RPGEnchantingTableBlock::new);
	protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public MapCodec<RPGEnchantingTableBlock> getCodec() {
		return CODEC;
	}

	public RPGEnchantingTableBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		if (serverConfig.enable_ambient_enchant_particles.get()) {
			int rpg_enchanting_table_block_reach_radius = serverConfig.rpg_enchanting_table_block_reach_radius.get();
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
						if (random.nextInt(16) == 0 && world.getBlockState(blockPos).isIn(RPGEnchanting.ENCHANTING_PARTICLE_TARGETS)) {
							world.addParticle(
									ParticleTypes.ENCHANT,
									(double) pos.getX() + 0.5,
									(double) pos.getY() + 2.0,
									(double) pos.getZ() + 0.5,
									(float) i + random.nextFloat() - 0.5,
									(float) j - random.nextFloat() - 1.0F,
									(float) k + random.nextFloat() - 0.5
							);
						}
					}
				}
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

		return new ExtendedScreenHandlerFactory<>() {
			@Override
			public RPGEnchantmentScreenHandler.RPGEnchanterBlockData getScreenOpeningData(ServerPlayerEntity player) {
				return new RPGEnchantmentScreenHandler.RPGEnchanterBlockData(pos, prefix_enchantments, suffix_enchantments);
			}

			@Override
			public Text getDisplayName() {
				return Text.translatable("gui.rpg_enchanting_table.title");
			}

			@Nullable
			@Override
			public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
				return new RPGEnchantmentScreenHandler(syncId, playerInventory, pos, prefix_enchantments, suffix_enchantments);
			}
		};
	}

	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
