package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record UpdateEnchantingScreenPacket() implements CustomPayload {
	public static final Id<UpdateEnchantingScreenPacket> PACKET_ID = new Id<>(RPGEnchanting.identifier("update_enchanting_screen"));
	public static final PacketCodec<RegistryByteBuf, UpdateEnchantingScreenPacket> PACKET_CODEC = PacketCodec.of(UpdateEnchantingScreenPacket::write, UpdateEnchantingScreenPacket::new);

	public UpdateEnchantingScreenPacket(RegistryByteBuf registryByteBuf) {
		this();
	}

	private void write(RegistryByteBuf registryByteBuf) {
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return PACKET_ID;
	}
}