package com.github.theredbrain.rpgenchanting;

import com.github.theredbrain.rpgenchanting.config.ClientConfig;
import com.github.theredbrain.rpgenchanting.gui.screen.ingame.RPGEnchantmentScreen;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.rpgenchanting.render.block.entity.RPGEnchantingTableBlockEntityRenderer;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class RPGEnchantingClient implements ClientModInitializer {
	public static ClientConfig CLIENT_CONFIG;
	@Override
	public void onInitializeClient() {
		CLIENT_CONFIG = ConfigApiJava.registerAndLoadConfig(ClientConfig::new, RegisterType.CLIENT);

		BlockEntityRendererFactories.register(EntityRegistry.RPG_ENCHANTING_TABLE, RPGEnchantingTableBlockEntityRenderer::new);
		HandledScreens.register(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, RPGEnchantmentScreen::new);
	}
}