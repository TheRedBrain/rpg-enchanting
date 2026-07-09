package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateEnchantingScreenPacket() implements CustomPacketPayload {
	public static final Type<UpdateEnchantingScreenPacket> PACKET_ID = new Type<>(RPGEnchanting.identifier("update_enchanting_screen"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateEnchantingScreenPacket> PACKET_CODEC = StreamCodec.ofMember(UpdateEnchantingScreenPacket::write, UpdateEnchantingScreenPacket::new);

	public UpdateEnchantingScreenPacket(RegistryFriendlyByteBuf registryByteBuf) {
		this();
	}

	private void write(RegistryFriendlyByteBuf registryByteBuf) {
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PACKET_ID;
	}
}