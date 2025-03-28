package com.github.theredbrain.rpgenchanting;

import com.github.theredbrain.inventorysizeattributes.InventorySizeAttributesClient;
import com.github.theredbrain.rpgenchanting.config.ClientConfig;
import com.github.theredbrain.rpgenchanting.gui.screen.ingame.RPGEnchantmentScreen;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.rpgenchanting.render.block.entity.RPGEnchantingTableBlockEntityRenderer;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.Text;

public class RPGEnchantingClient implements ClientModInitializer {
	public static ClientConfig CLIENT_CONFIG;

	public static boolean showInactiveInventorySlots() {
		return RPGEnchanting.isInventorySizeAttributesLoaded ? InventorySizeAttributesClient.CLIENT_CONFIG.show_inactive_inventory_slots.get() : true;
	}

	@Override
	public void onInitializeClient() {
		CLIENT_CONFIG = ConfigApiJava.registerAndLoadConfig(ClientConfig::new, RegisterType.CLIENT);

		BlockEntityRendererFactories.register(EntityRegistry.RPG_ENCHANTING_TABLE, RPGEnchantingTableBlockEntityRenderer::new);
		HandledScreens.register(ScreenHandlerTypesRegistry.RPG_ENCHANTMENT_SCREEN_HANDLER, RPGEnchantmentScreen::new);
		initializeClientEvents();
	}

	public static void initializeClientEvents() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			ProfileComponent playerEnchantedComponent = stack.get(RPGEnchanting.PLAYER_ENCHANTED);
			if (playerEnchantedComponent != null && CLIENT_CONFIG.show_item_tooltip_enchanted_by_player_name.get()) {
				String formatting_config_string = CLIENT_CONFIG.item_tooltip_enchanted_by_player_name_formatting_string.get();
				StringBuilder formatting_string = new StringBuilder();
				if (!formatting_config_string.isEmpty()) {
					for (int i = 0; i < formatting_config_string.length(); i++) {
						formatting_string.append("§").append(formatting_config_string.charAt(i));
					}
				}
				lines.add(Text.translatable("item.additional_tooltip.player_relation.enchanted_by", formatting_string + playerEnchantedComponent.gameProfile().getName()));
			}
		});
	}
}