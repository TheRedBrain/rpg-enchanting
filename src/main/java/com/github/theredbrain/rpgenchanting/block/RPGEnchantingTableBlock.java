package com.github.theredbrain.rpgenchanting.block;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class RPGEnchantingTableBlock extends BaseEntityBlock {
	public static final MapCodec<RPGEnchantingTableBlock> CODEC = simpleCodec(RPGEnchantingTableBlock::new);
	protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public MapCodec<RPGEnchantingTableBlock> codec() {
		return CODEC;
	}

	public RPGEnchantingTableBlock(BlockBehaviour.Properties settings) {
		super(settings);
	}

	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		super.animateTick(state, world, pos, random);
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;
		if (serverConfig.enable_ambient_enchant_particles.get()) {
			int rpg_enchanting_table_block_reach_radius = serverConfig.ambient_enchant_particle_radius.get();
			for (int i = -rpg_enchanting_table_block_reach_radius; i <= rpg_enchanting_table_block_reach_radius; i++) {
				for (int j = -rpg_enchanting_table_block_reach_radius; j <= rpg_enchanting_table_block_reach_radius; j++) {
					for (int k = -rpg_enchanting_table_block_reach_radius; k <= rpg_enchanting_table_block_reach_radius; k++) {
						BlockPos blockPos = new BlockPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
						if (random.nextInt(16) == 0 && world.getBlockState(blockPos).is(RPGEnchanting.ENCHANTING_PARTICLE_TARGETS)) {
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

	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RPGEnchantingTableBlockEntity(pos, state);
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return world.isClientSide() ? createTickerHelper(type, EntityRegistry.RPG_ENCHANTING_TABLE, RPGEnchantingTableBlockEntity::tick) : null;
	}

	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		if (world.isClientSide()) {
			return InteractionResult.SUCCESS;
		} else if (world.getBlockEntity(pos) instanceof RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity) {
			player.openMenu(createRPGEnchanterBlockScreenHandlerFactory(
					rpgEnchantingTableBlockEntity.getBlockPos(),
					((Nameable)rpgEnchantingTableBlockEntity).getDisplayName(),
					rpgEnchantingTableBlockEntity.getBookCost(),
					rpgEnchantingTableBlockEntity.getEnchantingMode(),
					rpgEnchantingTableBlockEntity.getAdvancementEnchantments(player),
					rpgEnchantingTableBlockEntity.getBlockEnchantments(),
					rpgEnchantingTableBlockEntity.getBookEnchantments()
			));
		}
		return InteractionResult.CONSUME;
	}

	public static MenuProvider createRPGEnchanterBlockScreenHandlerFactory(
			BlockPos blockPos,
			Component title,
			BookCost bookCost,
			EnchantmentUnlockMode enchantmentUnlockMode,
			Set<MutablePair<String, Integer>> advancement_enchantments,
			Set<MutablePair<String, Integer>> block_enchantments,
			Set<MutablePair<String, Integer>> book_enchantments
	) {
		return new ExtendedScreenHandlerFactory<>() {
			@Override
			public RPGEnchantmentScreenHandler.RPGEnchanterBlockData getScreenOpeningData(ServerPlayer player) {
				return new RPGEnchantmentScreenHandler.RPGEnchanterBlockData(blockPos, bookCost, enchantmentUnlockMode, advancement_enchantments, block_enchantments, book_enchantments);
			}

			@Override
			public Component getDisplayName() {
				return title;
			}

			@Nullable
			@Override
			public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
				return new RPGEnchantmentScreenHandler(syncId, playerInventory, blockPos, bookCost, enchantmentUnlockMode, advancement_enchantments, block_enchantments, book_enchantments);
			}
		};
	}

	public enum EnchantmentUnlockMode implements StringRepresentable {
		ADDITION("addition"),
		BLOCK_REQUIRED_FOR_ADVANCEMENT("block_required_for_advancement");

		private final String name;

		EnchantmentUnlockMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public static Optional<EnchantmentUnlockMode> byName(String name) {
			return Arrays.stream(EnchantmentUnlockMode.values()).filter(enchantmentUnlockMode -> enchantmentUnlockMode.getSerializedName().equals(name)).findFirst();
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

	public enum BookCost implements StringRepresentable {
		CONSUME("consume"),
		PARTIAL_CONSUME("partial_consume"),
		KEEP("keep");

		private final String name;

		BookCost(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public static Optional<BookCost> byName(String name) {
			return Arrays.stream(BookCost.values()).filter(bookCost -> bookCost.getSerializedName().equals(name)).findFirst();
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

	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
