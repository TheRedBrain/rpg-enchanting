package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class EntityRegistry {

	public static final BlockEntityType<RPGEnchantingTableBlockEntity> RPG_ENCHANTING_TABLE = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			RPGEnchanting.identifier("rpg_enchanting_table"),
			FabricBlockEntityTypeBuilder.create(RPGEnchantingTableBlockEntity::new, BlockRegistry.RPG_ENCHANTING_TABLE_BLOCK).build());

	public static void init() {
	}
}
