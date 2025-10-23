package com.github.theredbrain.rpgenchanting.gui.screen.ingame;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.RPGEnchantingClient;
import com.github.theredbrain.rpgenchanting.config.ClientConfig;
import com.github.theredbrain.rpgenchanting.config.ServerConfig;
import com.github.theredbrain.rpgenchanting.network.packet.RPGEnchantItemPacket;
import com.github.theredbrain.rpgenchanting.network.packet.UpdateEnchantingScreenPacket;
import com.github.theredbrain.rpgenchanting.screen.RPGEnchantmentScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CyclingSlotIcon;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.MathHelper;
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

		if (this.client == null) {
			this.close();
		}
		this.BOOK_MODEL = new BookModel(this.client.getLoadedEntityModels().getModelPart(EntityModelLayers.BOOK));

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

		Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.handler.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);

		if (optionalEnchantmentRegistry.isPresent()) {
			IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = optionalEnchantmentRegistry.get().getIndexedEntries();

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
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		int i = (this.width - this.backgroundWidth) / 2;
		int j = (this.height - this.backgroundHeight) / 2;
		this.prefixEnchantmentsMouseClicked = false;
		this.suffixEnchantmentsMouseClicked = false;

		if (this.client != null && this.client.interactionManager != null) {
			int threshold = this.handler.current_prefix_enchantments.size();
			for (int k = 0; k < Math.min(4, threshold); k++) {
				double d = click.x() - (double) (i + 8);
				double e = click.y() - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(true, k + this.prefixEnchantmentsScrollPosition);
					return true;
				}
			}

			for (int k = 0; k < Math.min(4, this.handler.current_suffix_enchantments.size()); k++) {
				double d = click.x() - (double) (i + 168);
				double e = click.y() - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(false, k + this.suffixEnchantmentsScrollPosition);
					return true;
				}
			}

		}

		if (this.handler.current_prefix_enchantments.size() > 4) {
			i = this.x + 119;
			j = this.y + 59;
			if (click.x() >= (double) i && click.x() < (double) (i + 6) && click.y() >= (double) j && click.y() < (double) (j + 76)) {
				this.prefixEnchantmentsMouseClicked = true;
			}
		}
		if (this.handler.current_suffix_enchantments.size() > 4) {
			i = this.x + 159;
			j = this.y + 59;
			if (click.x() >= (double) i && click.x() < (double) (i + 6) && click.y() >= (double) j && click.y() < (double) (j + 76)) {
				this.suffixEnchantmentsMouseClicked = true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (this.handler.current_prefix_enchantments.size() > 4
				&& this.prefixEnchantmentsMouseClicked) {
			int i = this.handler.current_prefix_enchantments.size() - 4;
			float f = (float) offsetY / (float) i;
			this.prefixEnchantmentsScrollAmount = MathHelper.clamp(this.prefixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.prefixEnchantmentsScrollPosition = (int) ((double) (this.prefixEnchantmentsScrollAmount * (float) i));
		}
		if (this.handler.current_suffix_enchantments.size() > 4
				&& this.suffixEnchantmentsMouseClicked) {
			int i = this.handler.current_suffix_enchantments.size() - 4;
			float f = (float) offsetY / (float) i;
			this.suffixEnchantmentsScrollAmount = MathHelper.clamp(this.suffixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.suffixEnchantmentsScrollPosition = (int) ((double) (this.suffixEnchantmentsScrollAmount * (float) i));
		}
		return super.mouseDragged(click, offsetX, offsetY);
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

		context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);

		if ((serverConfig.old_enchantment_item_cost_multiplier.get() > 0.0 || serverConfig.new_enchantment_item_cost_multiplier.get() > 0.0) && (!serverConfig.prefix_item_cost.get().equals(Identifier.of("minecraft:air")) || !serverConfig.suffix_item_cost.get().equals(Identifier.of("minecraft:air")))) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 133, y + 61, 0, 0, 18, 18, 18, 18);
			this.itemCostSlotIcon.render(this.handler, context, delta, this.x, this.y);
		}

		boolean showInactiveSlots = RPGEnchantingClient.showInactiveInventorySlots();
		for (k = 0; k < (showInactiveSlots ? 27 : Math.min(this.inventorySize, 27)); ++k) {
			m = (k / 9);
			context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 61 + (k - (m * 9)) * 18, y + 150 + (m * 18), 0, 0, 18, 18, 18, 18);
		}
		for (k = 0; k < (showInactiveSlots ? 9 : Math.min(this.hotbarSize, 9)); ++k) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x + 61 + k * 18, y + 208, 0, 0, 18, 18, 18, 18);
		}

		this.drawBook(context, i, j);

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
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 18, 108, 19);

			context.drawWrappedText(this.textRenderer, prefixEnchantmentText, i + 10, j + 23, 106, -9937334, false);
		}
		if (existing_suffix_enchantment != null) {
			Optional<RegistryKey<Enchantment>> optionalSuffixEnchantmentKey = existing_suffix_enchantment.getLeft().getKey();
			MutableText suffixEnchantmentText = Text.empty();
			if (optionalSuffixEnchantmentKey.isPresent()) {
				suffixEnchantmentText = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalSuffixEnchantmentKey.get().getValue().toTranslationKey() + "." + existing_suffix_enchantment.getRight() + ".suffix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
			}
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 18, 108, 19);

			context.drawWrappedText(this.textRenderer, suffixEnchantmentText, i + 170, j + 23, 106, -9937334, false);
		}
		if (!current_prefix_enchantments.isEmpty()) {
			if (existing_prefix_enchantment != null) {
				context.drawText(this.textRenderer, REPLACE_ENCHANTMENT_TEXT, i + 10, j + 47, Colors.DARK_GRAY, false);
			} else {
				context.drawText(this.textRenderer, ADD_ENCHANTMENT_TEXT, i + 10, j + 47, Colors.DARK_GRAY, false);
			}

			int index = 0;
			for (int l = this.prefixEnchantmentsScrollPosition; l < Math.min(this.prefixEnchantmentsScrollPosition + 4, current_prefix_enchantments.size()); l++) {
				int r = mouseX - (i + 8);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = -9937334;
				MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_prefix_enchantments.get(l);
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
				MutableText text = Text.empty();

				int experience_cost_amount = this.handler.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.handler.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.handler.player.experienceLevel < experience_cost_amount || this.handler.getPrefixItemCount() < item_cost_amount) && !this.handler.player.isInCreativeMode();

				if (optionalRegistryKey.isPresent()) {
					text = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().getValue().toTranslationKey() + "." + entry.getRight() + ".prefix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				if (bl) {
					q = -12550384;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
					q = -128;
				} else {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
				}
				context.drawWrappedText(this.textRenderer, text, i + 10, j + 64 + index * 19, p, q, false);
				index++;
			}
		}
		if (!current_suffix_enchantments.isEmpty()) {
			if (existing_suffix_enchantment != null) {
				context.drawText(this.textRenderer, REPLACE_ENCHANTMENT_TEXT, i + 170, j + 47, Colors.DARK_GRAY, false);
			} else {
				context.drawText(this.textRenderer, ADD_ENCHANTMENT_TEXT, i + 170, j + 47, Colors.DARK_GRAY, false);
			}

			int index = 0;
			for (int l = this.suffixEnchantmentsScrollPosition; l < Math.min(this.suffixEnchantmentsScrollPosition + 4, current_suffix_enchantments.size()); l++) {
				int r = mouseX - (i + 168);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = -9937334;
				MutablePair<RegistryEntry.Reference<Enchantment>, Integer> entry = current_suffix_enchantments.get(l);
				Optional<RegistryKey<Enchantment>> optionalRegistryKey = entry.getLeft().getKey();
				MutableText text = Text.empty();

				int experience_cost_amount = this.handler.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxPower(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.handler.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.handler.player.experienceLevel < experience_cost_amount || this.handler.getSuffixItemCount() < item_cost_amount) && !this.handler.player.isInCreativeMode();

				if (optionalRegistryKey.isPresent()) {
					text = Text.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().getValue().toTranslationKey() + "." + entry.getRight() + ".suffix", Text.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				if (bl) {
					q = -12550384;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
					q = -128;
				} else {
					context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
				}
				context.drawWrappedText(this.textRenderer, text, i + 170, j + 64 + index * 19, p, q, false);
				index++;
			}
		}

		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				current_prefix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				x + 119,
				(int) (y + 59 + 69.0F * this.prefixEnchantmentsScrollAmount),
				6,
				7
		);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				current_suffix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				x + 159,
				(int) (y + 59 + 69.0F * this.suffixEnchantmentsScrollAmount),
				6,
				7
		);
	}

	private void drawBook(DrawContext context, int x, int y) {
		if (this.client != null) {
			float f = this.client.getRenderTickCounter().getTickProgress(false);
			float g = MathHelper.lerp(f, this.pageTurningSpeed, this.nextPageTurningSpeed);
			float h = MathHelper.lerp(f, this.pageAngle, this.nextPageAngle);
			int i = x + 123;
			int j = y + 7;
			int k = i + 38;
			int l = j + 31;
			context.addBookModel(this.BOOK_MODEL, BOOK_TEXTURE, 40.0F, g, h, i, j, k, l);
		}
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
				this.approximatePageAngle = this.approximatePageAngle + (this.random.nextInt(4) - this.random.nextInt(4));
			} while (this.nextPageAngle <= this.approximatePageAngle + 1.0F && this.nextPageAngle >= this.approximatePageAngle - 1.0F);
		}

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
