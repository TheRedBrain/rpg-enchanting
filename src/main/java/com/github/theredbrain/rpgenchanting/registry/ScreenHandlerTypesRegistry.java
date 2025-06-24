package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ScreenHandlerTypesRegistry {
	public static final ScreenHandlerType<RPGEnchantmentScreenHandler> RPG_ENCHANTMENT_SCREEN_HANDLER = new ScreenHandlerType<>(RPGEnchantmentScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

	public static void init() {
		Registry.register(Registries.SCREEN_HANDLER, RPGEnchanting.identifier("rpg_enchanting"), RPG_ENCHANTMENT_SCREEN_HANDLER);
	}
}
