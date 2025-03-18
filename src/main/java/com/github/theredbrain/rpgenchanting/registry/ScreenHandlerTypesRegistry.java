package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;

public class ScreenHandlerTypesRegistry {
	//	public static final ScreenHandlerType<RPGEnchantmentScreenHandler> RPG_ENCHANTMENT_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(RPGEnchantmentScreenHandler::new, RPGEnchantmentScreenHandler.ShopBlockData.PACKET_CODEC);
	public static final ExtendedScreenHandlerType<RPGEnchantmentScreenHandler, RPGEnchantmentScreenHandler.RPGEnchanterBlockData> RPG_ENCHANTMENT_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(RPGEnchantmentScreenHandler::new, RPGEnchantmentScreenHandler.RPGEnchanterBlockData.PACKET_CODEC);
//	public static final ScreenHandlerType<RPGEnchantmentScreenHandler> RPG_ENCHANTMENT_SCREEN_HANDLER = new ScreenHandlerType<>(RPGEnchantmentScreenHandler::new, FeatureSet.of(FeatureFlags.VANILLA));

	public static void init() {
		Registry.register(Registries.SCREEN_HANDLER, RPGEnchanting.identifier("rpg_enchanting"), RPG_ENCHANTMENT_SCREEN_HANDLER);
	}
}
