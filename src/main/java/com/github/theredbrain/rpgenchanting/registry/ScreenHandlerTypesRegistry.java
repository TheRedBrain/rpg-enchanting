package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class ScreenHandlerTypesRegistry {
	public static final ExtendedScreenHandlerType<RPGEnchantmentScreenHandler, RPGEnchantmentScreenHandler.RPGEnchanterBlockData> RPG_ENCHANTMENT_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(RPGEnchantmentScreenHandler::new, RPGEnchantmentScreenHandler.RPGEnchanterBlockData.PACKET_CODEC);

	public static void init() {
		Registry.register(BuiltInRegistries.MENU, RPGEnchanting.identifier("rpg_enchanting"), RPG_ENCHANTMENT_SCREEN_HANDLER);
	}
}
