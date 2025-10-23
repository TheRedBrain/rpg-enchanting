package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.List;

public class BlockRegistry {

	public static RegistryKey<Block> RPG_ENCHANTING_TABLE_BLOCK_KEY = RegistryKey.of(RegistryKeys.BLOCK, RPGEnchanting.identifier("rpg_enchanting_table"));
	public static RegistryKey<Item> RPG_ENCHANTING_TABLE_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, RPGEnchanting.identifier("rpg_enchanting_table"));
	public static final Block RPG_ENCHANTING_TABLE_BLOCK = registerBlock(RPG_ENCHANTING_TABLE_BLOCK_KEY, RPG_ENCHANTING_TABLE_ITEM_KEY, new RPGEnchantingTableBlock(AbstractBlock.Settings.create().registryKey(RPG_ENCHANTING_TABLE_BLOCK_KEY).mapColor(MapColor.RED).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().luminance(state -> 7).strength(5.0F, 1200.0F)), List.of(ItemGroups.FUNCTIONAL));

	private static Block registerBlock(RegistryKey<Block> block_key, RegistryKey<Item> item_key, Block block, List<RegistryKey<ItemGroup>> itemGroupList) {
		Registry.register(Registries.ITEM, item_key, new BlockItem(block, new Item.Settings().registryKey(item_key)));
		for (RegistryKey<ItemGroup> itemGroup : itemGroupList) {
			ItemGroupEvents.modifyEntriesEvent(itemGroup).register(content -> content.add(block));
		}
		return Registry.register(Registries.BLOCK, block_key, block);
	}

	public static void init() {
	}
}
