package com.github.theredbrain.rpgenchanting;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPGEnchanting implements ModInitializer {
	public static final String MOD_ID = "rpgenchanting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("RPG-ifying the enchanting system!");
	}
}