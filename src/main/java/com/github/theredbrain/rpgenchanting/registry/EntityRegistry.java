package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class EntityRegistry {

	public static final BlockEntityType<RPGEnchantingTableBlockEntity> RPG_ENCHANTING_TABLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			RPGEnchanting.identifier("rpg_enchanting_table"),
			FabricBlockEntityTypeBuilder.create(RPGEnchantingTableBlockEntity::new, BlockRegistry.RPG_ENCHANTING_TABLE_BLOCK).build());

	public static void init() {
	}
}
