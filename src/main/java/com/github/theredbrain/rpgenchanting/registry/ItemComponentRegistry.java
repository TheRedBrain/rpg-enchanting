package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Unit;

public class ItemComponentRegistry {
	static {
		RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				RPGEnchanting.identifier("show_enchantment_name_additions"),
				ComponentType.<Unit>builder().codec(Unit.CODEC).packetCodec(PacketCodec.unit(Unit.INSTANCE)).cache().build()
		);
		RPGEnchanting.PLAYER_ENCHANTED = Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				RPGEnchanting.identifier("player_enchanted"),
				ComponentType.<ProfileComponent>builder().codec(ProfileComponent.CODEC).packetCodec(ProfileComponent.PACKET_CODEC).cache().build()
		);
	}

	public static void init() {
	}
}
