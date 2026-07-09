package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import java.util.List;

public class BlockRegistry {

	public static ResourceKey<Block> RPG_ENCHANTING_TABLE_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, RPGEnchanting.identifier("rpg_enchanting_table"));
	public static ResourceKey<Item> RPG_ENCHANTING_TABLE_ITEM_KEY = ResourceKey.create(Registries.ITEM, RPGEnchanting.identifier("rpg_enchanting_table"));
	public static final Block RPG_ENCHANTING_TABLE_BLOCK = registerBlock(RPG_ENCHANTING_TABLE_BLOCK_KEY, RPG_ENCHANTING_TABLE_ITEM_KEY, new RPGEnchantingTableBlock(BlockBehaviour.Properties.of().setId(RPG_ENCHANTING_TABLE_BLOCK_KEY).mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().lightLevel(state -> 7).strength(5.0F, 1200.0F)), List.of(CreativeModeTabs.FUNCTIONAL_BLOCKS));

	private static Block registerBlock(ResourceKey<Block> block_key, ResourceKey<Item> item_key, Block block, List<ResourceKey<CreativeModeTab>> itemGroupList) {
		Registry.register(BuiltInRegistries.ITEM, item_key, new BlockItem(block, new Item.Properties().setId(item_key)));
		for (ResourceKey<CreativeModeTab> itemGroup : itemGroupList) {
			CreativeModeTabEvents.modifyOutputEvent(itemGroup).register(content -> content.accept(block));
		}
		return Registry.register(BuiltInRegistries.BLOCK, block_key, block);
	}

	public static void init() {
	}
}
