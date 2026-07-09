package com.github.theredbrain.rpgenchanting.registry;

import com.github.theredbrain.rpgenchanting.network.packet.RPGEnchantItemPacket;
import com.github.theredbrain.rpgenchanting.network.packet.RPGEnchantItemPacketReceiver;
import com.github.theredbrain.rpgenchanting.network.packet.UpdateEnchantingScreenPacket;
import com.github.theredbrain.rpgenchanting.network.packet.UpdateEnchantingScreenPacketReceiver;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ServerPacketRegistry {

	public static void init() {

		PayloadTypeRegistry.serverboundPlay().register(RPGEnchantItemPacket.PACKET_ID, RPGEnchantItemPacket.PACKET_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RPGEnchantItemPacket.PACKET_ID, new RPGEnchantItemPacketReceiver());

		PayloadTypeRegistry.serverboundPlay().register(UpdateEnchantingScreenPacket.PACKET_ID, UpdateEnchantingScreenPacket.PACKET_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UpdateEnchantingScreenPacket.PACKET_ID, new UpdateEnchantingScreenPacketReceiver());

	}
}
