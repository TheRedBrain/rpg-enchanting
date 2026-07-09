package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RPGEnchantItemPacket(
		BlockPos blockPos,
		int enchantmentId,
		int enchantmentLevel,
		boolean shouldConsumeBook,
		boolean isPrefix
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RPGEnchantItemPacket> PACKET_ID = new CustomPacketPayload.Type<>(RPGEnchanting.identifier("rpg_enchant_item"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RPGEnchantItemPacket> PACKET_CODEC = StreamCodec.ofMember(RPGEnchantItemPacket::write, RPGEnchantItemPacket::new);

	public RPGEnchantItemPacket(RegistryFriendlyByteBuf registryByteBuf) {
		this(
				registryByteBuf.readBlockPos(),
				registryByteBuf.readInt(),
				registryByteBuf.readInt(),
				registryByteBuf.readBoolean(),
				registryByteBuf.readBoolean()
		);
	}

	private void write(RegistryFriendlyByteBuf registryByteBuf) {
		registryByteBuf.writeBlockPos(this.blockPos);
		registryByteBuf.writeInt(this.enchantmentId);
		registryByteBuf.writeInt(this.enchantmentLevel);
		registryByteBuf.writeBoolean(this.shouldConsumeBook);
		registryByteBuf.writeBoolean(this.isPrefix);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return PACKET_ID;
	}
}
