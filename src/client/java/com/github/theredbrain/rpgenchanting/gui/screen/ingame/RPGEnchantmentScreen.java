package com.github.theredbrain.rpgenchanting.gui.screen.ingame;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.RPGEnchantingClient;
import com.github.theredbrain.rpgenchanting.config.ClientConfig;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.network.packet.RPGEnchantItemPacket;
import com.github.theredbrain.rpgenchanting.network.packet.UpdateEnchantingScreenPacket;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CyclingSlotIcon;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class RPGEnchantmentScreen extends HandledScreen<RPGEnchantmentScreenHandler> {

	private static final Identifier ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier ENCHANTMENT_SLOT_TEXTURE = Identifier.ofVanilla("container/enchanting_table/enchantment_slot");
	public static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("textures/gui/sprites/container/slot.png");
	private static final Identifier SCROLLER_VERTICAL_6_7_TEXTURE = RPGEnchanting.identifier("scroll_bar/scroller_vertical_6_7");
	private static final Identifier SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE = RPGEnchanting.identifier("scroll_bar/scroller_vertical_6_7_disabled");
	private static final Identifier TEXTURE = RPGEnchanting.identifier("textures/gui/container/rpg_enchanting_table.png");
	private static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/entity/enchanting_table_book.png");
	private static final Identifier PREFIX_ITEM_COST_SLOT_TEXTURE = RPGEnchanting.identifier("item/prefix_slot_item_cost");
	private static final Identifier SUFFIX_ITEM_COST_SLOT_TEXTURE = RPGEnchanting.identifier("item/suffix_slot_item_cost");
	private static final List<Identifier> ITEM_COST_SLOT_TEXTURES = List.of(
			PREFIX_ITEM_COST_SLOT_TEXTURE, SUFFIX_ITEM_COST_SLOT_TEXTURE
	);
	private static final Text ADD_ENCHANTMENT_TEXT = Text.translatable("gui.rpg_enchanting_table.add_enchantment");
	private static final Text REPLACE_ENCHANTMENT_TEXT = Text.translatable("gui.rpg_enchanting_table.replace_enchantment");
	private final CyclingSlotIcon itemCostSlotIcon = new CyclingSlotIcon(1);
	private final Random random = Random.create();
	private BookModel BOOK_MODEL;
	public int ticks;
	public float nextPageAngle;
	public float pageAngle;
	public float approximatePageAngle;
	public float pageRotationSpeed;
	public float nextPageTurningSpeed;
	public float pageTurningSpeed;
	private ItemStack stack = ItemStack.EMPTY;
	private final int hotbarSize;
	private final int inventorySize;
	private int prefixEnchantmentsScrollPosition = 0;
	private float prefixEnchantmentsScrollAmount = 0.0f;
	private boolean prefixEnchantmentsMouseClicked = false;
	private int suffixEnchantmentsScrollPosition = 0;
	private float suffixEnchantmentsScrollAmount = 0.0f;
	private boolean suffixEnchantmentsMouseClicked = false;

	public RPGEnchantmentScreen(RPGEnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.hotbarSize = RPGEnchanting.getActiveHotbarSize(inventory.player);
		this.inventorySize = RPGEnchanting.getActiveInventorySize(inventory.player);
	}

	@Override
	protected void init() {
		this.backgroundWidth = 284;
		this.backgroundHeight = 233;

		this.playerInventoryTitleX = 62;
		this.playerInventoryTitleY = 139;

		super.init();

		this.BOOK_MODEL = new BookModel(this.client.getEntityModelLoader().getModelPart(EntityModelLayers.BOOK));

		this.prefixEnchantmentsScrollPosition = 0;
		this.prefixEnchantmentsScrollAmount = 0.0f;
		this.suffixEnchantmentsScrollPosition = 0;
		this.suffixEnchantmentsScrollAmount = 0.0f;
		this.itemCostSlotIcon.updateTexture(ITEM_COST_SLOT_TEXTURES);
	}

	@Override
	public void handledScreenTick() {
		super.handledScreenTick();
		if (RPGEnchantingClient.CLIENT_CONFIG.enable_texture_cycling_for_item_cost_slot.get()) {
			this.itemCostSlotIcon.updateTexture(ITEM_COST_SLOT_TEXTURES);
		}
		this.doTick();
	}

	public void enchant(boolean isPrefix, int index) {

		IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = this.handler.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getIndexedEntries();

		MutablePair<RegistryEntry.Reference<Enchantment>, Integer> newEnchantment = isPrefix ? this.handler.current_prefix_enchantments.get(index) : this.handler.current_suffix_enchantments.get(index);

		ClientPlayNetworking.send(new RPGEnchantItemPacket(
				this.handler.blockPos,
				indexedIterable.getRawId(newEnchantment.getLeft()),
				newEnchantment.getRight(),
				this.handler.consumable_enchantments.contains(newEnchantment),
				isPrefix
		));

		ClientPlayNetworking.send(new UpdateEnchantingScreenPacket());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int i = this.x;
		int j = this.y;
		this.prefixEnchantmentsMouseClicked = false;
		this.suffixEnchantmentsMouseClicked = false;

		if (this.client != null && this.client.interactionManager != null) {
			int threshold = this.handler.current_prefix_enchantments.size();
			for (int k = 0; k < Math.min(4, threshold); k++) {
				double d = mouseX - (double) (i + 8);
				double e = mouseY - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(true, k + this.prefixEnchantmentsScrollPosition);
					return true;
				}
			}

			for (int k = 0; k < Math.min(4, this.handler.current_suffix_enchantments.size()); k++) {
				double d = mouseX - (double) (i + 168);
				double e = mouseY - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(false, k + this.suffixEnchantmentsScrollPosition);
					return true;
				}
			}

		}

		if (this.handler.current_prefix_enchantments.size() > 4) {
			i = this.x + 119;
			j = this.y + 59;
			if (mouseX >= (double) i && mouseX < (double) (i + 6) && mouseY >= (double) j && mouseY < (double) (j + 76)) {
				this.prefixEnchantmentsMouseClicked = true;
			}
		}
		if (this.handler.current_suffix_enchantments.size() > 4) {
			i = this.x + 159;
			j = this.y + 59;
			if (mouseX >= (double) i && mouseX < (double) (i + 6) && mouseY >= (double) j && mouseY < (double) (j + 76)) {
				this.suffixEnchantmentsMouseClicked = true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.handler.current_prefix_enchantments.size() > 4
				&& this.prefixEnchantmentsMouseClicked) {
			int i = this.handler.current_prefix_enchantments.size() - 4;
			float f = (float) deltaY / (float) i;
			this.prefixEnchantmentsScrollAmount = MathHelper.clamp(this.prefixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.prefixEnchantmentsScrollPosition = (int) ((double) (this.prefixEnchantmentsScrollAmount * (float) i));
		}
		if (this.handler.current_suffix_enchantments.size() > 4
				&& this.suffixEnchantmentsMouseClicked) {
			int i = this.handler.current_suffix_enchantments.size() - 4;
			float f = (float) deltaY / (float) i;
			this.suffixEnchantmentsScrollAmount = MathHelper.clamp(this.suffixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.suffixEnchantmentsScrollPosition = (int) ((double) (this.suffixEnchantmentsScrollAmount * (float) i));
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.handler.current_prefix_enchantments.size() > 4
				&& mouseX >= (double) (this.x + 7) && mouseX <= (double) (this.x + 126)
				&& mouseY >= (double) (this.y + 58) && mouseY <= (double) (this.y + 136)) {
			int i = this.handler.current_prefix_enchantments.size() - 4;
			float f = (float) verticalAmount / (float) i;
			this.prefixEnchantmentsScrollAmount = MathHelper.clamp(this.prefixEnchantmentsScrollAmount - f, 0.0f, 1.0f);
			this.prefixEnchantmentsScrollPosition = (int) ((double) (this.prefixEnchantmentsScrollAmount * (float) i));
		}
		if (this.handler.current_suffix_enchantments.size() > 4
				&& mouseX >= (double) (this.x + 158) && mouseX <= (double) (this.x + 277)
				&& mouseY >= (double) (this.y + 58) && mouseY <= (double) (this.y + 136)) {
			int i = this.handler.current_suffix_enchantments.size() - 4;
			float f = (float) verticalAmount / (float) i;
			this.suffixEnchantmentsScrollAmount = MathHelper.clamp(this.suffixEnchantmentsScrollAmount - f, 0.0f, 1.0f);
			this.suffixEnchantmentsScrollPosition = (int) ((double) (this.suffixEnchantmentsScrollAmount * (float) i));
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int i = this.x;
		int j = this.y;
		int k;
		int m;
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);

		if (serverConfig.old_enchantment_item_cost_multiplier.get() > 0.0 || serverConfig.new_enchantment_item_cost_multiplier.get() > 0.0) {
			context.drawTexture(SLOT_TEXTURE, x + 133, y + 61, 0, 0, 18, 18, 18, 18);
		}

		boolean showInactiveSlots = RPGEnchantingClient.showInactiveInventorySlots();
		for (k = 0; k < (showInactiveSlots ? 27 : Math.min(this.inventorySize, 27)); ++k) {
			m = (k / 9);
			context.drawTexture(SLOT_TEXTURE, x + 61 + (k - (m * 9)) * 18, y + 150 + (m * 18), 0, 0, 18, 18, 18, 18);
		}
		for (k = 0; k < (showInactiveSlots ? 9 : Math.min(this.hotbarSize, 9)); ++k) {
			context.drawTexture(SLOT_TEXTURE, x + 61 + k * 18, y + 208, 0, 0, 18, 18, 18, 18);
		}

		this.itemCostSlotIcon.render(this.handler, context, delta, this.x, this.y);

		this.drawBook(context, i, j, delta);

		MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = this.handler.existing_prefix_enchantment;
		MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = this.handler.existing_suffix_enchantment;
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = this.handler.current_prefix_enchantments;
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = this.handler.current_suffix_enchantments;
		if (existing_prefix_enchantment != null) {
			Optional<RegistryKey<Enchantment>> optionalPrefixEnchantmentKey = existing_prefix_enchantment.getLeft().getKey();
			MutableText prefixEnchantmentText = Text.empty();
			if (optionalPrefixEnchantmentKey.isPresent()) {
				prefixEnchantmentText = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalPrefixEnchantmentKey.get().getValue().toTranslationKey() + "." + existing_prefix_enchantment.getRight() + ".prefix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
			}
			RenderSystem.enableBlend();
			context.drawGuiTexture(ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 18, 108, 19);
			RenderSystem.disableBlend();

			context.drawTextWrapped(this.textRenderer, prefixEnchantmentText, i + 10, j + 23, 106, 6839882);
		}
		if (existing_suffix_enchantment != null) {
			Optional<RegistryKey<Enchantment>> optionalSuffixEnchantmentKey = existing_suffix_enchantment.getLeft().getKey();
			MutableText suffixEnchantmentText = Text.empty();
			if (optionalSuffixEnchantmentKey.isPresent()) {
				suffixEnchantmentText = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalSuffixEnchantmentKey.get().getValue().toTranslationKey() + "." + existing_suffix_enchantment.getRight() + ".suffix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
			}
			RenderSystem.enableBlend();
			context.drawGuiTexture(ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 18, 108, 19);
			RenderSystem.disableBlend();

			context.drawTextWrapped(this.textRenderer, suffixEnchantmentText, i + 170, j + 23, 106, 6839882);
		}
		if (!current_prefix_enchantments.isEmpty()) {
			if (existing_prefix_enchantment != null) {
				context.drawText(this.textRenderer, REPLACE_ENCHANTMENT_TEXT, i + 10, j + 47, 4210752, false);
			} else {
				context.drawText(this.textRenderer, ADD_ENCHANTMENT_TEXT, i + 10, j + 47, 4210752, false);
			}

			int index = 0;
			for (int l = this.prefixEnchantmentsScrollPosition; l < Math.min(this.prefixEnchantmentsScrollPosition + 4, current_prefix_enchantments.size()); l++) {
				int r = mouseX - (i + 8);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = 6839882;
				MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_prefix_enchantments.get(l);
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
				MutableText text = Text.empty();

				int experience_cost_amount = this.handler.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.handler.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.handler.player.experienceLevel < experience_cost_amount || this.handler.getPrefixItemCount() < item_cost_amount) && !this.handler.player.isInCreativeMode();

				if (optionalRegistryKey.isPresent()) {
					text = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().getValue().toTranslationKey() + "." + entry.getRight() + ".prefix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				RenderSystem.enableBlend();
				if (bl) {
					q = (q & 16711422) >> 1;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.drawGuiTexture(ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
					q = 16777088;
				} else {
					context.drawGuiTexture(ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
				}
				RenderSystem.disableBlend();
				context.drawTextWrapped(this.textRenderer, text, i + 10, j + 64 + index * 19, p, q);
				index++;
			}
		}
		if (!current_suffix_enchantments.isEmpty()) {
			if (existing_suffix_enchantment != null) {
				context.drawText(this.textRenderer, REPLACE_ENCHANTMENT_TEXT, i + 170, j + 47, 4210752, false);
			} else {
				context.drawText(this.textRenderer, ADD_ENCHANTMENT_TEXT, i + 170, j + 47, 4210752, false);
			}

			int index = 0;
			for (int l = this.suffixEnchantmentsScrollPosition; l < Math.min(this.suffixEnchantmentsScrollPosition + 4, current_suffix_enchantments.size()); l++) {
				int r = mouseX - (i + 168);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = 6839882;
				MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_suffix_enchantments.get(l);
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
				MutableText text = Text.empty();

				int experience_cost_amount = this.handler.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.handler.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.handler.player.experienceLevel < experience_cost_amount || this.handler.getSuffixItemCount() < item_cost_amount) && !this.handler.player.isInCreativeMode();

				if (optionalRegistryKey.isPresent()) {
					text = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().getValue().toTranslationKey() + "." + entry.getRight() + ".suffix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				RenderSystem.enableBlend();
				if (bl) {
					q = (q & 16711422) >> 1;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.drawGuiTexture(ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
					q = 16777088;
				} else {
					context.drawGuiTexture(ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
				}
				RenderSystem.disableBlend();
				context.drawTextWrapped(this.textRenderer, text, i + 170, j + 64 + index * 19, p, q);
				index++;
			}
		}

		context.drawGuiTexture(
				current_prefix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				x + 119,
				(int) (y + 59 + 69.0F * this.prefixEnchantmentsScrollAmount),
				6,
				7
		);
		context.drawGuiTexture(
				current_suffix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				x + 159,
				(int) (y + 59 + 69.0F * this.suffixEnchantmentsScrollAmount),
				6,
				7
		);
	}

	private void drawBook(DrawContext context, int x, int y, float delta) {
		float f = MathHelper.lerp(delta, this.pageTurningSpeed, this.nextPageTurningSpeed);
		float g = MathHelper.lerp(delta, this.pageAngle, this.nextPageAngle);
		DiffuseLighting.method_34742();
		context.getMatrices().push();
		context.getMatrices().translate((float) x + 142.0F, (float) y + 25.0F, 100.0F);
		float h = 40.0F;
		context.getMatrices().scale(-40.0F, 40.0F, 40.0F);
		context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(25.0F));
		context.getMatrices().translate((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
		float i = -(1.0F - f) * 90.0F - 90.0F;
		context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i));
		context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
		float j = MathHelper.clamp(MathHelper.fractionalPart(g + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
		float k = MathHelper.clamp(MathHelper.fractionalPart(g + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
		this.BOOK_MODEL.setPageAngles(0.0F, j, k, f);
		VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(this.BOOK_MODEL.getLayer(BOOK_TEXTURE));
		this.BOOK_MODEL.render(context.getMatrices(), vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV);
		context.draw();
		context.getMatrices().pop();
		DiffuseLighting.enableGuiDepthLighting();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
		int i = this.x;
		int j = this.y;
		int k;
		int m;
		boolean bl = this.handler.player.isInCreativeMode();
		ClientConfig clientConfig = RPGEnchantingClient.CLIENT_CONFIG;
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_prefix_enchantment = this.handler.existing_prefix_enchantment;
		MutablePair<RegistryEntry.Reference<Enchantment>, Integer> existing_suffix_enchantment = this.handler.existing_suffix_enchantment;
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_prefix_enchantments = this.handler.current_prefix_enchantments;
		List<MutablePair<RegistryEntry.Reference<Enchantment>, Integer>> current_suffix_enchantments = this.handler.current_suffix_enchantments;
		if (existing_prefix_enchantment != null) {
			if (this.isPointWithinBounds(8, 18, 108, 19, mouseX, mouseY)) {
				List<Text> list = new ArrayList<>();
				list.add(Enchantment.getName(existing_prefix_enchantment.getLeft(), existing_prefix_enchantment.getRight()));
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = existing_prefix_enchantment.getLeft().getKey();
				if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
					list.add(Text.translatable("enchantment." + optionalRegistryKey.get().getValue().toTranslationKey() + ".desc").formatted(Formatting.GRAY));
				}
				context.drawTooltip(this.textRenderer, list, mouseX, mouseY);
				return;
			}
		}
		if (existing_suffix_enchantment != null) {
			if (this.isPointWithinBounds(168, 18, 108, 19, mouseX, mouseY)) {
				List<Text> list = new ArrayList<>();
				list.add(Enchantment.getName(existing_suffix_enchantment.getLeft(), existing_suffix_enchantment.getRight()));
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = existing_suffix_enchantment.getLeft().getKey();
				if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
					list.add(Text.translatable("enchantment." + optionalRegistryKey.get().getValue().toTranslationKey() + ".desc").formatted(Formatting.GRAY));
				}
				context.drawTooltip(this.textRenderer, list, mouseX, mouseY);
				return;
			}
		}
		if (!current_prefix_enchantments.isEmpty()) {
			int index = 0;
			int prefixItemCount = this.handler.getPrefixItemCount();
			int experienceLevel = this.handler.player.experienceLevel;

			for (int l = this.prefixEnchantmentsScrollPosition; l < Math.min(this.prefixEnchantmentsScrollPosition + 4, current_prefix_enchantments.size()); l++) {
				if (this.isPointWithinBounds(8, 59 + index * 19, 108, 19, mouseX, mouseY)) {
					MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_prefix_enchantments.get(l);
					List<Text> list = new ArrayList<>();
					list.add(Enchantment.getName(entry.getLeft(), entry.getRight()));
					Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
					if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
						list.add(Text.translatable("enchantment." + optionalRegistryKey.get().getValue().toTranslationKey() + ".desc").formatted(Formatting.GRAY));
					}
					if (!bl) {
						list.add(ScreenTexts.EMPTY);
						int experience_cost_amount = this.handler.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
						int item_cost_amount = this.handler.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (item_cost_amount > 0) {
							MutableText mutableText = Text.literal(item_cost_amount + " ").append(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.prefix_item_cost.get()).asItem().getName());
							list.add(mutableText.formatted(prefixItemCount >= item_cost_amount ? Formatting.GRAY : Formatting.RED));
						}

						if (experience_cost_amount > 0) {
							MutableText mutableText2;
							if (experience_cost_amount == 1) {
								mutableText2 = Text.translatable("container.enchant.level.one");
							} else {
								mutableText2 = Text.translatable("container.enchant.level.many", new Object[]{experience_cost_amount});
							}
							list.add(mutableText2.formatted(experienceLevel >= experience_cost_amount ? Formatting.GRAY : Formatting.RED));
						}
					}

					context.drawTooltip(this.textRenderer, list, mouseX, mouseY);
					return;
				}
				index++;
			}
		}
		if (!current_suffix_enchantments.isEmpty()) {
			int index = 0;
			int suffixItemCount = this.handler.getSuffixItemCount();
			int experienceLevel = this.handler.player.experienceLevel;

			for (int l = this.suffixEnchantmentsScrollPosition; l < Math.min(this.suffixEnchantmentsScrollPosition + 4, current_suffix_enchantments.size()); l++) {
				if (this.isPointWithinBounds(168, 59 + index * 19, 108, 19, mouseX, mouseY)) {
					MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_suffix_enchantments.get(l);
					List<Text> list = new ArrayList<>();
					list.add(Enchantment.getName(entry.getLeft(), entry.getRight()));
					Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
					if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
						list.add(Text.translatable("enchantment." + optionalRegistryKey.get().getValue().toTranslationKey() + ".desc").formatted(Formatting.GRAY));
					}
					if (!bl) {
						list.add(ScreenTexts.EMPTY);
						int experience_cost_amount = this.handler.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
						int item_cost_amount = this.handler.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (item_cost_amount > 0) {
							MutableText mutableText = Text.literal(item_cost_amount + " ").append(Registries.ITEM.get(RPGEnchanting.SERVER_CONFIG.suffix_item_cost.get()).asItem().getName());
							list.add(mutableText.formatted(suffixItemCount >= item_cost_amount ? Formatting.GRAY : Formatting.RED));
						}

						if (experience_cost_amount > 0) {
							MutableText mutableText2;
							if (experience_cost_amount == 1) {
								mutableText2 = Text.translatable("container.enchant.level.one");
							} else {
								mutableText2 = Text.translatable("container.enchant.level.many", new Object[]{experience_cost_amount});
							}
							list.add(mutableText2.formatted(experienceLevel >= experience_cost_amount ? Formatting.GRAY : Formatting.RED));
						}
					}

					context.drawTooltip(this.textRenderer, list, mouseX, mouseY);
					return;
				}
				index++;
			}
		}
	}

	public void doTick() {
		ItemStack itemStack = this.handler.getSlot(0).getStack();
		if (!ItemStack.areEqual(itemStack, this.stack)) {
			this.stack = itemStack;

			do {
				this.approximatePageAngle = this.approximatePageAngle + (float) (this.random.nextInt(4) - this.random.nextInt(4));
			} while (this.nextPageAngle <= this.approximatePageAngle + 1.0F && this.nextPageAngle >= this.approximatePageAngle - 1.0F);
		}

		this.ticks++;
		this.pageAngle = this.nextPageAngle;
		this.pageTurningSpeed = this.nextPageTurningSpeed;

		if (this.handler.existing_prefix_enchantment != null || this.handler.existing_suffix_enchantment != null) {
			this.nextPageTurningSpeed += 0.2F;
		} else {
			this.nextPageTurningSpeed -= 0.2F;
		}

		this.nextPageTurningSpeed = MathHelper.clamp(this.nextPageTurningSpeed, 0.0F, 1.0F);
		float f = (this.approximatePageAngle - this.nextPageAngle) * 0.4F;
		float g = 0.2F;
		f = MathHelper.clamp(f, -0.2F, 0.2F);
		this.pageRotationSpeed = this.pageRotationSpeed + (f - this.pageRotationSpeed) * 0.9F;
		this.nextPageAngle = this.nextPageAngle + this.pageRotationSpeed;
	}
}
