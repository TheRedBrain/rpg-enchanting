package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ScreenHandlerTypesRegistry {
	public static final ExtendedScreenHandlerType<RPGEnchantmentScreenHandler, RPGEnchantmentScreenHandler.RPGEnchanterBlockData> RPG_ENCHANTMENT_SCREEN_HANDLER = new ExtendedScreenHandlerType<>(RPGEnchantmentScreenHandler::new, RPGEnchantmentScreenHandler.RPGEnchanterBlockData.PACKET_CODEC);

	public static void init() {
		Registry.register(Registries.SCREEN_HANDLER, RPGEnchanting.identifier("rpg_enchanting"), RPG_ENCHANTMENT_SCREEN_HANDLER);
	}
}
