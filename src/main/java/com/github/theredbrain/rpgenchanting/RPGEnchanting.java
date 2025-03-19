package com.github.theredbrain.rpgenchanting;

import com.github.theredbrain.inventorysizeattributes.entity.player.DuckPlayerEntityMixin;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.registry.BlockRegistry;
import com.github.theredbrain.rpgenchanting.registry.EntityRegistry;
import com.github.theredbrain.rpgenchanting.registry.ItemComponentRegistry;
import com.github.theredbrain.rpgenchanting.registry.ScreenHandlerTypesRegistry;
import io.netty.buffer.ByteBuf;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RPGEnchanting implements ModInitializer {
	public static final String MOD_ID = "rpgenchanting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ServerConfig SERVER_CONFIG;

	public static TagKey<Block> ENCHANTING_PARTICLE_TARGETS = TagKey.of(RegistryKeys.BLOCK, identifier("enchanting_particle_targets"));

	public static TagKey<Enchantment> PREFIX_ENCHANTMENTS = TagKey.of(RegistryKeys.ENCHANTMENT, identifier("prefix_enchantments"));
	public static TagKey<Enchantment> SUFFIX_ENCHANTMENTS = TagKey.of(RegistryKeys.ENCHANTMENT, identifier("suffix_enchantments"));

	public static TagKey<Item> ENCHANTING_PREFIX_COST_ITEMS = TagKey.of(RegistryKeys.ITEM, identifier("enchanting_prefix_cost_items"));
	public static TagKey<Item> ENCHANTING_SUFFIX_COST_ITEMS = TagKey.of(RegistryKeys.ITEM, identifier("enchanting_suffix_cost_items"));

	public static ComponentType<Unit> SHOW_ENCHANTMENT_NAME_ADDITIONS;

	public static final boolean isInventorySizeAttributesLoaded = FabricLoader.getInstance().isModLoaded("inventorysizeattributes");

	public static int getActiveInventorySize(PlayerEntity player) {
		return isInventorySizeAttributesLoaded ? ((DuckPlayerEntityMixin) player).inventorysizeattributes$getActiveInventorySlotAmount() : 27;
	}

	public static int getActiveHotbarSize(PlayerEntity player) {
		return isInventorySizeAttributesLoaded ? ((DuckPlayerEntityMixin) player).inventorysizeattributes$getActiveHotbarSlotAmount() : 9;
	}

	public static final PacketCodec<ByteBuf, MutablePair<String, Integer>> MUTABLE_PAIR_STRING_INTEGER = new PacketCodec<>() {
		public MutablePair<String, Integer> decode(ByteBuf byteBuf) {
			return new MutablePair<>(
					PacketCodecs.STRING.decode(byteBuf),
					PacketCodecs.INTEGER.decode(byteBuf)
			);
		}

		public void encode(ByteBuf byteBuf, MutablePair<String, Integer> pairIdentifierEntityAttributeModifier) {
			PacketCodecs.STRING.encode(byteBuf, pairIdentifierEntityAttributeModifier.getLeft());
			PacketCodecs.INTEGER.encode(byteBuf, pairIdentifierEntityAttributeModifier.getRight());
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
		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
		if (modContainer.isPresent()) {

			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_vanilla"), modContainer.get(), Text.translatable("resourcepack.rpgenchanting.rpgenchanting_vanilla.name"), ResourcePackActivationType.DEFAULT_ENABLED);
			ResourceManagerHelper.registerBuiltinResourcePack(identifier("rpgenchanting_vanilla_resources"), modContainer.get(), Text.translatable("resourcepack.rpgenchanting.rpgenchanting_vanilla_resources.name"), ResourcePackActivationType.DEFAULT_ENABLED);
		}
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}