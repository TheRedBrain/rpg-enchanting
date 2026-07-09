package com.github.theredbrain.rpgenchanting.render.block.entity;

import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.render.block.entity.state.RPGEnchantingTableBlockEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RPGEnchantingTableBlockEntityRenderer implements BlockEntityRenderer<RPGEnchantingTableBlockEntity, RPGEnchantingTableBlockEntityRenderState> {
	public static final Material BOOK_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("enchanting_table_book");
	private final MaterialSet spriteHolder;
	private final BookModel book;

	public RPGEnchantingTableBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
		this.spriteHolder = ctx.materials();
		this.book = new BookModel(ctx.bakeLayer(ModelLayers.BOOK));
	}

	public RPGEnchantingTableBlockEntityRenderState createRenderState() {
		return new RPGEnchantingTableBlockEntityRenderState();
	}

	public void extractRenderState(
			RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity,
			RPGEnchantingTableBlockEntityRenderState rpgEnchantingTableBlockEntityRenderState,
			float f,
			Vec3 vec3d,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.extractRenderState(rpgEnchantingTableBlockEntity, rpgEnchantingTableBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
		rpgEnchantingTableBlockEntityRenderState.pageAngle = Mth.lerp(f, rpgEnchantingTableBlockEntity.pageAngle, rpgEnchantingTableBlockEntity.nextPageAngle);
		rpgEnchantingTableBlockEntityRenderState.pageTurningSpeed = Mth.lerp(
				f, rpgEnchantingTableBlockEntity.pageTurningSpeed, rpgEnchantingTableBlockEntity.nextPageTurningSpeed
		);
		rpgEnchantingTableBlockEntityRenderState.ticks = rpgEnchantingTableBlockEntity.ticks + f;
		float g = rpgEnchantingTableBlockEntity.bookRotation - rpgEnchantingTableBlockEntity.lastBookRotation;

		while (g >= (float) Math.PI) {
			g -= (float) (Math.PI * 2);
		}

		while (g < (float) -Math.PI) {
			g += (float) (Math.PI * 2);
		}

		rpgEnchantingTableBlockEntityRenderState.bookRotationDegrees = rpgEnchantingTableBlockEntity.lastBookRotation + g * f;
	}

	public void submit(
			RPGEnchantingTableBlockEntityRenderState rpgEnchantingTableBlockEntityRenderState,
			PoseStack matrixStack,
			SubmitNodeCollector orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.pushPose();
		matrixStack.translate(0.5F, 0.75F, 0.5F);
		matrixStack.translate(0.0F, 0.1F + Mth.sin(rpgEnchantingTableBlockEntityRenderState.ticks * 0.1F) * 0.01F, 0.0F);
		float f = rpgEnchantingTableBlockEntityRenderState.bookRotationDegrees;
		matrixStack.mulPose(Axis.YP.rotation(-f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
		float g = Mth.frac(rpgEnchantingTableBlockEntityRenderState.pageAngle + 0.25F) * 1.6F - 0.3F;
		float h = Mth.frac(rpgEnchantingTableBlockEntityRenderState.pageAngle + 0.75F) * 1.6F - 0.3F;
		BookModel.State bookModelState = new BookModel.State(
				rpgEnchantingTableBlockEntityRenderState.ticks,
				Mth.clamp(g, 0.0F, 1.0F),
				Mth.clamp(h, 0.0F, 1.0F),
				rpgEnchantingTableBlockEntityRenderState.pageTurningSpeed
		);
		orderedRenderCommandQueue.submitModel(
				this.book,
				bookModelState,
				matrixStack,
				BOOK_TEXTURE.renderType(RenderTypes::entitySolid),
				rpgEnchantingTableBlockEntityRenderState.lightCoords,
				OverlayTexture.NO_OVERLAY,
				-1,
				this.spriteHolder.get(BOOK_TEXTURE),
				0,
				rpgEnchantingTableBlockEntityRenderState.breakProgress
		);
		matrixStack.popPose();
	}
}
