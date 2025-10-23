package com.github.theredbrain.rpgenchanting.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;

@Environment(EnvType.CLIENT)
public class RPGEnchantingTableBlockEntityRenderState extends BlockEntityRenderState {
	public float ticks;
	public float bookRotationDegrees;
	public float pageAngle;
	public float pageTurningSpeed;
}
