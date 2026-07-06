package com.github.theredbrain.rpgenchanting.render.block.entity;

import com.github.theredbrain.rpgenchanting.block.entitiy.RPGEnchantingTableBlockEntity;
import com.github.theredbrain.rpgenchanting.render.block.entity.state.RPGEnchantingTableBlockEntityRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RPGEnchantingTableBlockEntityRenderer implements BlockEntityRenderer<RPGEnchantingTableBlockEntity, RPGEnchantingTableBlockEntityRenderState> {
	public static final SpriteIdentifier BOOK_TEXTURE = TexturedRenderLayers.ENTITY_SPRITE_MAPPER.mapVanilla("enchanting_table_book");
	private final SpriteHolder spriteHolder;
	private final BookModel book;

	public RPGEnchantingTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.spriteHolder = ctx.spriteHolder();
		this.book = new BookModel(ctx.getLayerModelPart(EntityModelLayers.BOOK));
	}

	public RPGEnchantingTableBlockEntityRenderState createRenderState() {
		return new RPGEnchantingTableBlockEntityRenderState();
	}

	public void updateRenderState(
			RPGEnchantingTableBlockEntity rpgEnchantingTableBlockEntity,
			RPGEnchantingTableBlockEntityRenderState rpgEnchantingTableBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			@Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(rpgEnchantingTableBlockEntity, rpgEnchantingTableBlockEntityRenderState, f, vec3d, crumblingOverlayCommand);
		rpgEnchantingTableBlockEntityRenderState.pageAngle = MathHelper.lerp(f, rpgEnchantingTableBlockEntity.pageAngle, rpgEnchantingTableBlockEntity.nextPageAngle);
		rpgEnchantingTableBlockEntityRenderState.pageTurningSpeed = MathHelper.lerp(
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

	public void render(
			RPGEnchantingTableBlockEntityRenderState rpgEnchantingTableBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		matrixStack.translate(0.5F, 0.75F, 0.5F);
		matrixStack.translate(0.0F, 0.1F + MathHelper.sin(rpgEnchantingTableBlockEntityRenderState.ticks * 0.1F) * 0.01F, 0.0F);
		float f = rpgEnchantingTableBlockEntityRenderState.bookRotationDegrees;
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(-f));
		matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(80.0F));
		float g = MathHelper.fractionalPart(rpgEnchantingTableBlockEntityRenderState.pageAngle + 0.25F) * 1.6F - 0.3F;
		float h = MathHelper.fractionalPart(rpgEnchantingTableBlockEntityRenderState.pageAngle + 0.75F) * 1.6F - 0.3F;
		BookModel.BookModelState bookModelState = new BookModel.BookModelState(
				rpgEnchantingTableBlockEntityRenderState.ticks,
				MathHelper.clamp(g, 0.0F, 1.0F),
				MathHelper.clamp(h, 0.0F, 1.0F),
				rpgEnchantingTableBlockEntityRenderState.pageTurningSpeed
		);
		orderedRenderCommandQueue.submitModel(
				this.book,
				bookModelState,
				matrixStack,
				BOOK_TEXTURE.getRenderLayer(RenderLayers::entitySolid),
				rpgEnchantingTableBlockEntityRenderState.lightmapCoordinates,
				OverlayTexture.DEFAULT_UV,
				-1,
				this.spriteHolder.getSprite(BOOK_TEXTURE),
				0,
				rpgEnchantingTableBlockEntityRenderState.crumblingOverlay
		);
		matrixStack.pop();
	}
}
