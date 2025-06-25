package com.github.theredbrain.rpgenchanting.network.packet;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record RPGEnchantItemPacket(
		BlockPos blockPos,
		int enchantmentId,
		int enchantmentLevel,
		boolean shouldConsumeBook,
		boolean isPrefix
) implements CustomPayload {
	public static final CustomPayload.Id<RPGEnchantItemPacket> PACKET_ID = new CustomPayload.Id<>(RPGEnchanting.identifier("rpg_enchant_item"));
	public static final PacketCodec<RegistryByteBuf, RPGEnchantItemPacket> PACKET_CODEC = PacketCodec.of(RPGEnchantItemPacket::write, RPGEnchantItemPacket::new);

	public RPGEnchantItemPacket(RegistryByteBuf registryByteBuf) {
		this(
				registryByteBuf.readBlockPos(),
				registryByteBuf.readInt(),
				registryByteBuf.readInt(),
				registryByteBuf.readBoolean(),
				registryByteBuf.readBoolean()
		);
	}

	private void write(RegistryByteBuf registryByteBuf) {
		registryByteBuf.writeBlockPos(this.blockPos);
		registryByteBuf.writeInt(this.enchantmentId);
		registryByteBuf.writeInt(this.enchantmentLevel);
		registryByteBuf.writeBoolean(this.shouldConsumeBook);
		registryByteBuf.writeBoolean(this.isPrefix);
	}

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return PACKET_ID;
	}
}
