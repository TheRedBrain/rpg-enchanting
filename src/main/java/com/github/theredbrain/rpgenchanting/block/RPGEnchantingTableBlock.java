package com.github.theredbrain.rpgenchanting.block;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Nameable;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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
			int rpg_enchanting_table_block_reach_radius = serverConfig.ambient_enchant_particle_radius.get();
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
						if (random.nextInt(16) == 0 && world.getBlockState(blockPos).isIn(RPGEnchanting.ENCHANTING_PARTICLE_TARGETS)) {
							world.addParticleClient(
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
		return world.isClient() ? validateTicker(type, EntityRegistry.RPG_ENCHANTING_TABLE, RPGEnchantingTableBlockEntity::tick) : null;
	}

	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		} else if (world.getBlockEntity(pos) instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
			player.openHandledScreen(createRPGEnchanterBlockScreenHandlerFactory(
					rpgEnchantingTableBlockEntity.getPos(),
					((Nameable)rpgEnchantingTableBlockEntity).getDisplayName(),
					rpgEnchantingTableBlockEntity.getBookCost(),
					rpgEnchantingTableBlockEntity.getEnchantingMode(),
					rpgEnchantingTableBlockEntity.getAdvancementEnchantments(player),
					rpgEnchantingTableBlockEntity.getBlockEnchantments(),
					rpgEnchantingTableBlockEntity.getBookEnchantments()
			));
		}
		return ActionResult.CONSUME;
	}

	public static NamedScreenHandlerFactory createRPGEnchanterBlockScreenHandlerFactory(
			BlockPos blockPos,
			Text title,
			RPGEnchantingTableBlock.BookCost bookCost,
			EnchantmentUnlockMode enchantmentUnlockMode,
			Set<MutablePair<String, Integer>> advancement_enchantments,
			Set<MutablePair<String, Integer>> block_enchantments,
			Set<MutablePair<String, Integer>> book_enchantments
	) {
		return new ExtendedScreenHandlerFactory<>() {
			@Override
			public RPGEnchantmentScreenHandler.RPGEnchanterBlockData getScreenOpeningData(ServerPlayerEntity player) {
				return new RPGEnchantmentScreenHandler.RPGEnchanterBlockData(blockPos, bookCost, enchantmentUnlockMode, advancement_enchantments, block_enchantments, book_enchantments);
			}

			@Override
			public Text getDisplayName() {
				return title;
			}

			@Nullable
			@Override
			public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
				return new RPGEnchantmentScreenHandler(syncId, playerInventory, blockPos, bookCost, enchantmentUnlockMode, advancement_enchantments, block_enchantments, book_enchantments);
			}
		};
	}

	public enum EnchantmentUnlockMode implements StringIdentifiable {
		ADDITION("addition"),
		BLOCK_REQUIRED_FOR_ADVANCEMENT("block_required_for_advancement");

		private final String name;

		EnchantmentUnlockMode(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}

		public static Optional<EnchantmentUnlockMode> byName(String name) {
			return Arrays.stream(EnchantmentUnlockMode.values()).filter(enchantmentUnlockMode -> enchantmentUnlockMode.asString().equals(name)).findFirst();
		}

		@Nullable
		public static RPGEnchantingTableBlock.EnchantmentUnlockMode valueOfOrNull(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	public enum BookCost implements StringIdentifiable {
		CONSUME("consume"),
		PARTIAL_CONSUME("partial_consume"),
		KEEP("keep");

		private final String name;

		BookCost(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return this.name;
		}

		public static Optional<BookCost> byName(String name) {
			return Arrays.stream(BookCost.values()).filter(bookCost -> bookCost.asString().equals(name)).findFirst();
		}

		@Nullable
		public static BookCost valueOfOrNull(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
