package com.github.theredbrain.rpgenchanting;

import com.github.theredbrain.inventorysizeattributes.entity.player.DuckPlayerEntityMixin;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.BlockRegistry;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.registry.ItemComponentRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import com.github.theredbrain.rpgenchanting.registry.ServerPacketRegistry;
import io.netty.buffer.ByteBuf;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RPGEnchanting implements ModInitializer {
	public static final String MOD_ID = "rpgenchanting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ServerConfig SERVER_CONFIG;

	public static TagKey<Block> ENCHANTING_PARTICLE_TARGETS = TagKey.create(Registries.BLOCK, identifier("enchanting_particle_targets"));

	public static TagKey<Enchantment> PREFIX_ENCHANTMENTS = TagKey.create(Registries.ENCHANTMENT, identifier("prefix_enchantments"));
	public static TagKey<Enchantment> SUFFIX_ENCHANTMENTS = TagKey.create(Registries.ENCHANTMENT, identifier("suffix_enchantments"));

	public static DataComponentType<Unit> SHOW_ENCHANTMENT_NAME_ADDITIONS;
	public static DataComponentType<ResolvableProfile> PLAYER_ENCHANTED;

	public static final boolean isInventorySizeAttributesLoaded = FabricLoader.getInstance().isModLoaded("inventorysizeattributes");

	public static int getActiveInventorySize(Player player) {
		return isInventorySizeAttributesLoaded ? ((DuckPlayerEntityMixin) player).inventorysizeattributes$getActiveInventorySlotAmount() : 27;
	}

	public static int getActiveHotbarSize(Player player) {
		return isInventorySizeAttributesLoaded ? ((DuckPlayerEntityMixin) player).inventorysizeattributes$getActiveHotbarSlotAmount() : 9;
	}

	public static final StreamCodec<ByteBuf, MutablePair<String, Integer>> MUTABLE_PAIR_STRING_INTEGER = new StreamCodec<>() {
		public MutablePair<String, Integer> decode(ByteBuf byteBuf) {
			return new MutablePair<>(
					ByteBufCodecs.STRING_UTF8.decode(byteBuf),
					ByteBufCodecs.INT.decode(byteBuf)
			);
		}

		public void encode(ByteBuf byteBuf, MutablePair<String, Integer> pairIdentifierEntityAttributeModifier) {
			ByteBufCodecs.STRING_UTF8.encode(byteBuf, pairIdentifierEntityAttributeModifier.getLeft());
			ByteBufCodecs.INT.encode(byteBuf, pairIdentifierEntityAttributeModifier.getRight());
		}
	};

	@Override
	public void onInitialize() {
		LOGGER.info("RPG-ifying the enchanting system!");
		SERVER_CONFIG = ConfigApiJava.registerAndLoadConfig(ServerConfig::new, RegisterType.BOTH);

		BlockRegistry.init();
		EntityRegistry.init();
		ItemComponentRegistry.init();
		ScreenHandlerTypesRegistry.init();
		ServerPacketRegistry.init();

		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
		if (modContainer.isPresent()) {
			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_dungeons_and_taverns"), modContainer.get(), Component.translatable("resourcepack.rpgenchanting.rpgenchanting_dungeons_and_taverns.name"), ResourcePackActivationType.DEFAULT_ENABLED);
//			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_extra_spell_attributes"), modContainer.get(), Text.translatable("resourcepack.rpgenchanting.rpgenchanting_extra_spell_attributes.name"), ResourcePackActivationType.DEFAULT_ENABLED);
//			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_more_rpg_series"), modContainer.get(), Text.translatable("resourcepack.rpgenchanting.rpgenchanting_more_rpg_series.name"), ResourcePackActivationType.DEFAULT_ENABLED);
//			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_rpg_series"), modContainer.get(), Text.translatable("resourcepack.rpgenchanting.rpgenchanting_rpg_series.name"), ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_vanilla"), modContainer.get(), Component.translatable("resourcepack.rpgenchanting.rpgenchanting_vanilla.name"), ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_replace_vanilla_enchanting_table_recipe"), modContainer.get(), Component.translatable("resourcepack.rpgenchanting.rpgenchanting_replace_vanilla_enchanting_table_recipe.name"), ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_compatibility_resources"), modContainer.get(), Component.translatable("resourcepack.rpgenchanting.rpgenchanting_compatibility_resources.name"), ResourcePackActivationType.DEFAULT_ENABLED);
		}
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void info(String message) {
		LOGGER.info("[" + MOD_ID + "] [info]: " + message);
	}

}