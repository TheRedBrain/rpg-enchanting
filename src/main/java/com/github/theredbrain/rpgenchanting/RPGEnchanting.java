package com.github.theredbrain.rpgenchanting;

import com.github.theredbrain.rpgenchanting.registry.BlockRegistry;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.registry.ItemComponentRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import net.fabricmc.api.ModInitializer;

import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPGEnchanting implements ModInitializer {
	public static final String MOD_ID = "rpgenchanting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static TagKey<Enchantment> PREFIX_ENCHANTMENTS = TagKey.of(RegistryKeys.ENCHANTMENT, identifier("prefix_enchantments"));
	public static TagKey<Enchantment> SUFFIX_ENCHANTMENTS = TagKey.of(RegistryKeys.ENCHANTMENT, identifier("suffix_enchantments"));

	public static ComponentType<Unit> SHOW_ENCHANTMENT_NAME_ADDITIONS;

	@Override
	public void onInitialize() {
		LOGGER.info("RPG-ifying the enchanting system!");

		BlockRegistry.init();
		EntityRegistry.init();
		ItemComponentRegistry.init();
		ScreenHandlerTypesRegistry.init();
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}