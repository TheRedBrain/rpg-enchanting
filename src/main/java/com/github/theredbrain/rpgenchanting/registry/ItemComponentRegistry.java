package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.ResolvableProfile;

public class ItemComponentRegistry {
	static {
		RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS = Registry.register(
				BuiltInRegistries.DATA_COMPONENT_TYPE,
				RPGEnchanting.identifier("show_enchantment_name_additions"),
				DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)).cacheEncoding().build()
		);
		RPGEnchanting.PLAYER_ENCHANTED = Registry.register(
				BuiltInRegistries.DATA_COMPONENT_TYPE,
				RPGEnchanting.identifier("player_enchanted"),
				DataComponentType.<ResolvableProfile>builder().persistent(ResolvableProfile.CODEC).networkSynchronized(ResolvableProfile.STREAM_CODEC).cacheEncoding().build()
		);
	}

	public static void init() {
	}
}
