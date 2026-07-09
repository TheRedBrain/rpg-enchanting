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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class RPGEnchantmentScreen extends AbstractContainerScreen<RPGEnchantmentScreenHandler> {

	private static final Identifier ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier ENCHANTMENT_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/enchanting_table/enchantment_slot");
	public static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/container/slot.png");
	private static final Identifier SCROLLER_VERTICAL_6_7_TEXTURE = RPGEnchanting.identifier("scroll_bar/scroller_vertical_6_7");
	private static final Identifier SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE = RPGEnchanting.identifier("scroll_bar/scroller_vertical_6_7_disabled");
	private static final Identifier TEXTURE = RPGEnchanting.identifier("textures/gui/container/rpg_enchanting_table.png");
	private static final Identifier BOOK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enchanting_table_book.png");
	private static final Identifier PREFIX_ITEM_COST_SLOT_TEXTURE = RPGEnchanting.identifier("container/slot/prefix_slot_item_cost");
	private static final Identifier SUFFIX_ITEM_COST_SLOT_TEXTURE = RPGEnchanting.identifier("container/slot/suffix_slot_item_cost");
	private static final List<Identifier> ITEM_COST_SLOT_TEXTURES = List.of(
			PREFIX_ITEM_COST_SLOT_TEXTURE, SUFFIX_ITEM_COST_SLOT_TEXTURE
	);
	private static final Component ADD_ENCHANTMENT_TEXT = Component.translatable("gui.rpg_enchanting_table.add_enchantment");
	private static final Component REPLACE_ENCHANTMENT_TEXT = Component.translatable("gui.rpg_enchanting_table.replace_enchantment");
	private final CyclingSlotBackground itemCostSlotIcon = new CyclingSlotBackground(1);
	private final RandomSource random = RandomSource.create();
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

	public RPGEnchantmentScreen(RPGEnchantmentScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.hotbarSize = RPGEnchanting.getActiveHotbarSize(inventory.player);
		this.inventorySize = RPGEnchanting.getActiveInventorySize(inventory.player);
	}

	@Override
	protected void init() {
		this.imageWidth = 284;
		this.imageHeight = 233;

		this.inventoryLabelX = 62;
		this.inventoryLabelY = 139;

		super.init();

		if (this.minecraft == null) {
			this.onClose();
		}
		this.BOOK_MODEL = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));

		this.prefixEnchantmentsScrollPosition = 0;
		this.prefixEnchantmentsScrollAmount = 0.0f;
		this.suffixEnchantmentsScrollPosition = 0;
		this.suffixEnchantmentsScrollAmount = 0.0f;
		this.itemCostSlotIcon.tick(ITEM_COST_SLOT_TEXTURES);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (RPGEnchantingClient.CLIENT_CONFIG.enable_texture_cycling_for_item_cost_slot.get()) {
			this.itemCostSlotIcon.tick(ITEM_COST_SLOT_TEXTURES);
		}
		this.doTick();
	}

	public void enchant(boolean isPrefix, int index) {

		Optional<Registry<Enchantment>> optionalEnchantmentRegistry = this.menu.world.getRegistryManager().getOptional(Registries.ENCHANTMENT);

		if (optionalEnchantmentRegistry.isPresent()) {
			IdMap<Holder<Enchantment>> indexedIterable = optionalEnchantmentRegistry.get().asHolderIdMap();

			MutablePair<Holder.Reference<Enchantment>, Integer> newEnchantment = isPrefix ? this.menu.current_prefix_enchantments.get(index) : this.menu.current_suffix_enchantments.get(index);

			ClientPlayNetworking.send(new RPGEnchantItemPacket(
					this.menu.blockPos,
					indexedIterable.getId(newEnchantment.getLeft()),
					newEnchantment.getRight(),
					this.menu.consumable_enchantments.contains(newEnchantment),
					isPrefix
			));

			ClientPlayNetworking.send(new UpdateEnchantingScreenPacket());
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int i = (this.width - this.imageWidth) / 2;
		int j = (this.height - this.imageHeight) / 2;
		this.prefixEnchantmentsMouseClicked = false;
		this.suffixEnchantmentsMouseClicked = false;

		if (this.minecraft != null && this.minecraft.gameMode != null) {
			int threshold = this.menu.current_prefix_enchantments.size();
			for (int k = 0; k < Math.min(4, threshold); k++) {
				double d = click.x() - (double) (i + 8);
				double e = click.y() - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(true, k + this.prefixEnchantmentsScrollPosition);
					return true;
				}
			}

			for (int k = 0; k < Math.min(4, this.menu.current_suffix_enchantments.size()); k++) {
				double d = click.x() - (double) (i + 168);
				double e = click.y() - (double) (j + 59 + 19 * k);
				if (d >= 0.0 && e >= 0.0 && d < 108.0 && e < 19.0) {
					this.enchant(false, k + this.suffixEnchantmentsScrollPosition);
					return true;
				}
			}

		}

		if (this.menu.current_prefix_enchantments.size() > 4) {
			i = this.leftPos + 119;
			j = this.topPos + 59;
			if (click.x() >= (double) i && click.x() < (double) (i + 6) && click.y() >= (double) j && click.y() < (double) (j + 76)) {
				this.prefixEnchantmentsMouseClicked = true;
			}
		}
		if (this.menu.current_suffix_enchantments.size() > 4) {
			i = this.leftPos + 159;
			j = this.topPos + 59;
			if (click.x() >= (double) i && click.x() < (double) (i + 6) && click.y() >= (double) j && click.y() < (double) (j + 76)) {
				this.suffixEnchantmentsMouseClicked = true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (this.menu.current_prefix_enchantments.size() > 4
				&& this.prefixEnchantmentsMouseClicked) {
			int i = this.menu.current_prefix_enchantments.size() - 4;
			float f = (float) offsetY / (float) i;
			this.prefixEnchantmentsScrollAmount = Mth.clamp(this.prefixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.prefixEnchantmentsScrollPosition = (int) ((double) (this.prefixEnchantmentsScrollAmount * (float) i));
		}
		if (this.menu.current_suffix_enchantments.size() > 4
				&& this.suffixEnchantmentsMouseClicked) {
			int i = this.menu.current_suffix_enchantments.size() - 4;
			float f = (float) offsetY / (float) i;
			this.suffixEnchantmentsScrollAmount = Mth.clamp(this.suffixEnchantmentsScrollAmount + f, 0.0f, 1.0f);
			this.suffixEnchantmentsScrollPosition = (int) ((double) (this.suffixEnchantmentsScrollAmount * (float) i));
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.menu.current_prefix_enchantments.size() > 4
				&& mouseX >= (double) (this.leftPos + 7) && mouseX <= (double) (this.leftPos + 126)
				&& mouseY >= (double) (this.topPos + 58) && mouseY <= (double) (this.topPos + 136)) {
			int i = this.menu.current_prefix_enchantments.size() - 4;
			float f = (float) verticalAmount / (float) i;
			this.prefixEnchantmentsScrollAmount = Mth.clamp(this.prefixEnchantmentsScrollAmount - f, 0.0f, 1.0f);
			this.prefixEnchantmentsScrollPosition = (int) ((double) (this.prefixEnchantmentsScrollAmount * (float) i));
		}
		if (this.menu.current_suffix_enchantments.size() > 4
				&& mouseX >= (double) (this.leftPos + 158) && mouseX <= (double) (this.leftPos + 277)
				&& mouseY >= (double) (this.topPos + 58) && mouseY <= (double) (this.topPos + 136)) {
			int i = this.menu.current_suffix_enchantments.size() - 4;
			float f = (float) verticalAmount / (float) i;
			this.suffixEnchantmentsScrollAmount = Mth.clamp(this.suffixEnchantmentsScrollAmount - f, 0.0f, 1.0f);
			this.suffixEnchantmentsScrollPosition = (int) ((double) (this.suffixEnchantmentsScrollAmount * (float) i));
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
		int i = this.leftPos;
		int j = this.topPos;
		int k;
		int m;
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		if ((serverConfig.old_enchantment_item_cost_multiplier.get() > 0.0 || serverConfig.new_enchantment_item_cost_multiplier.get() > 0.0) && (!serverConfig.prefix_item_cost.get().equals(Identifier.parse("minecraft:air")) || !serverConfig.suffix_item_cost.get().equals(Identifier.parse("minecraft:air")))) {
			context.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, leftPos + 133, topPos + 61, 0, 0, 18, 18, 18, 18);
			this.itemCostSlotIcon.render(this.menu, context, delta, this.leftPos, this.topPos);
		}

		boolean showInactiveSlots = RPGEnchantingClient.showInactiveInventorySlots();
		for (k = 0; k < (showInactiveSlots ? 27 : Math.min(this.inventorySize, 27)); ++k) {
			m = (k / 9);
			context.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, leftPos + 61 + (k - (m * 9)) * 18, topPos + 150 + (m * 18), 0, 0, 18, 18, 18, 18);
		}
		for (k = 0; k < (showInactiveSlots ? 9 : Math.min(this.hotbarSize, 9)); ++k) {
			context.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, leftPos + 61 + k * 18, topPos + 208, 0, 0, 18, 18, 18, 18);
		}

		this.drawBook(context, i, j);

		MutablePair<Holder.Reference<Enchantment>, Integer> existing_prefix_enchantment = this.menu.existing_prefix_enchantment;
		MutablePair<Holder.Reference<Enchantment>, Integer> existing_suffix_enchantment = this.menu.existing_suffix_enchantment;
		List<MutablePair<Holder.Reference<Enchantment>, Integer>> current_prefix_enchantments = this.menu.current_prefix_enchantments;
		List<MutablePair<Holder.Reference<Enchantment>, Integer>> current_suffix_enchantments = this.menu.current_suffix_enchantments;
		if (existing_prefix_enchantment != null) {
			Optional<ResourceKey<Enchantment>> optionalPrefixEnchantmentKey = existing_prefix_enchantment.getLeft().unwrapKey();
			MutableComponent prefixEnchantmentText = Component.empty();
			if (optionalPrefixEnchantmentKey.isPresent()) {
				prefixEnchantmentText = Component.translatable(RPGEnchanting.MOD_ID + "." + optionalPrefixEnchantmentKey.get().identifier().toLanguageKey() + "." + existing_prefix_enchantment.getRight() + ".prefix", Component.translatable("gui.rpg_enchanting_table.placeholder"));
			}
			context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 18, 108, 19);

			context.drawWordWrap(this.font, prefixEnchantmentText, i + 10, j + 23, 106, -9937334, false);
		}
		if (existing_suffix_enchantment != null) {
			Optional<ResourceKey<Enchantment>> optionalSuffixEnchantmentKey = existing_suffix_enchantment.getLeft().unwrapKey();
			MutableComponent suffixEnchantmentText = Component.empty();
			if (optionalSuffixEnchantmentKey.isPresent()) {
				suffixEnchantmentText = Component.translatable(RPGEnchanting.MOD_ID + "." + optionalSuffixEnchantmentKey.get().identifier().toLanguageKey() + "." + existing_suffix_enchantment.getRight() + ".suffix", Component.translatable("gui.rpg_enchanting_table.placeholder"));
			}
			context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 18, 108, 19);

			context.drawWordWrap(this.font, suffixEnchantmentText, i + 170, j + 23, 106, -9937334, false);
		}
		if (!current_prefix_enchantments.isEmpty()) {
			if (existing_prefix_enchantment != null) {
				context.drawString(this.font, REPLACE_ENCHANTMENT_TEXT, i + 10, j + 47, CommonColors.DARK_GRAY, false);
			} else {
				context.drawString(this.font, ADD_ENCHANTMENT_TEXT, i + 10, j + 47, CommonColors.DARK_GRAY, false);
			}

			int index = 0;
			for (int l = this.prefixEnchantmentsScrollPosition; l < Math.min(this.prefixEnchantmentsScrollPosition + 4, current_prefix_enchantments.size()); l++) {
				int r = mouseX - (i + 8);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = -9937334;
				MutablePair<Holder.Reference<Enchantment>, Integer> entry = current_prefix_enchantments.get(l);
				Optional<ResourceKey<Enchantment>> optionalRegistryKey = entry.getLeft().unwrapKey();
				MutableComponent text = Component.empty();

				int experience_cost_amount = this.menu.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxCost(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.menu.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.menu.player.experienceLevel < experience_cost_amount || this.menu.getPrefixItemCount() < item_cost_amount) && !this.menu.player.isCreative();

				if (optionalRegistryKey.isPresent()) {
					text = Component.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().identifier().toLanguageKey() + "." + entry.getRight() + ".prefix", Component.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				if (bl) {
					q = -12550384;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
					q = -128;
				} else {
					context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 8, j + 59 + index * 19, 108, 19);
				}
				context.drawWordWrap(this.font, text, i + 10, j + 64 + index * 19, p, q, false);
				index++;
			}
		}
		if (!current_suffix_enchantments.isEmpty()) {
			if (existing_suffix_enchantment != null) {
				context.drawString(this.font, REPLACE_ENCHANTMENT_TEXT, i + 170, j + 47, CommonColors.DARK_GRAY, false);
			} else {
				context.drawString(this.font, ADD_ENCHANTMENT_TEXT, i + 170, j + 47, CommonColors.DARK_GRAY, false);
			}

			int index = 0;
			for (int l = this.suffixEnchantmentsScrollPosition; l < Math.min(this.suffixEnchantmentsScrollPosition + 4, current_suffix_enchantments.size()); l++) {
				int r = mouseX - (i + 168);
				int s = mouseY - (j + 59 + index * 19);
				int p = 106;
				int q = -9937334;
				MutablePair<Holder.Reference<Enchantment>, Integer> entry = current_suffix_enchantments.get(l);
				Optional<ResourceKey<Enchantment>> optionalRegistryKey = entry.getLeft().unwrapKey();
				MutableComponent text = Component.empty();

				int experience_cost_amount = this.menu.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxCost(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
				int item_cost_amount = this.menu.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));
				boolean bl = (this.menu.player.experienceLevel < experience_cost_amount || this.menu.getSuffixItemCount() < item_cost_amount) && !this.menu.player.isCreative();

				if (optionalRegistryKey.isPresent()) {
					text = Component.translatable(RPGEnchanting.MOD_ID + "." + optionalRegistryKey.get().identifier().toLanguageKey() + "." + entry.getRight() + ".suffix", Component.translatable("gui.rpg_enchanting_table.placeholder"));
				}
				if (bl) {
					q = -12550384;
				} else if (r >= 0 && s >= 0 && r < 108 && s < 19) {
					context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_HIGHLIGHTED_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
					q = -128;
				} else {
					context.blitSprite(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_SLOT_TEXTURE, i + 168, j + 59 + index * 19, 108, 19);
				}
				context.drawWordWrap(this.font, text, i + 170, j + 64 + index * 19, p, q, false);
				index++;
			}
		}

		context.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				current_prefix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				leftPos + 119,
				(int) (topPos + 59 + 69.0F * this.prefixEnchantmentsScrollAmount),
				6,
				7
		);
		context.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				current_suffix_enchantments.size() > 4 ? SCROLLER_VERTICAL_6_7_TEXTURE : SCROLLER_VERTICAL_6_7_DISABLED_TEXTURE,
				leftPos + 159,
				(int) (topPos + 59 + 69.0F * this.suffixEnchantmentsScrollAmount),
				6,
				7
		);
	}

	private void drawBook(GuiGraphics context, int x, int y) {
		if (this.minecraft != null) {
			float f = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
			float g = Mth.lerp(f, this.pageTurningSpeed, this.nextPageTurningSpeed);
			float h = Mth.lerp(f, this.pageAngle, this.nextPageAngle);
			int i = x + 123;
			int j = y + 7;
			int k = i + 38;
			int l = j + 31;
			context.submitBookModelRenderState(this.BOOK_MODEL, BOOK_TEXTURE, 40.0F, g, h, i, j, k, l);
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.renderTooltip(context, mouseX, mouseY);
		int i = this.leftPos;
		int j = this.topPos;
		int k;
		int m;
		boolean bl = this.menu.player.isCreative();
		ClientConfig clientConfig = RPGEnchantingClient.CLIENT_CONFIG;
		ServerConfig serverConfig = RPGEnchanting.SERVER_CONFIG;

		MutablePair<Holder.Reference<Enchantment>, Integer> existing_prefix_enchantment = this.menu.existing_prefix_enchantment;
		MutablePair<Holder.Reference<Enchantment>, Integer> existing_suffix_enchantment = this.menu.existing_suffix_enchantment;
		List<MutablePair<Holder.Reference<Enchantment>, Integer>> current_prefix_enchantments = this.menu.current_prefix_enchantments;
		List<MutablePair<Holder.Reference<Enchantment>, Integer>> current_suffix_enchantments = this.menu.current_suffix_enchantments;
		if (existing_prefix_enchantment != null) {
			if (this.isHovering(8, 18, 108, 19, mouseX, mouseY)) {
				List<Component> list = new ArrayList<>();
				list.add(Enchantment.getFullname(existing_prefix_enchantment.getLeft(), existing_prefix_enchantment.getRight()));
				Optional<ResourceKey<Enchantment>> optionalRegistryKey = existing_prefix_enchantment.getLeft().unwrapKey();
				if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
					list.add(Component.translatable("enchantment." + optionalRegistryKey.get().identifier().toLanguageKey() + ".desc").withStyle(ChatFormatting.GRAY));
				}
				context.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
				return;
			}
		}
		if (existing_suffix_enchantment != null) {
			if (this.isHovering(168, 18, 108, 19, mouseX, mouseY)) {
				List<Component> list = new ArrayList<>();
				list.add(Enchantment.getFullname(existing_suffix_enchantment.getLeft(), existing_suffix_enchantment.getRight()));
				Optional<ResourceKey<Enchantment>> optionalRegistryKey = existing_suffix_enchantment.getLeft().unwrapKey();
				if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
					list.add(Component.translatable("enchantment." + optionalRegistryKey.get().identifier().toLanguageKey() + ".desc").withStyle(ChatFormatting.GRAY));
				}
				context.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
				return;
			}
		}
		if (!current_prefix_enchantments.isEmpty()) {
			int index = 0;
			int prefixItemCount = this.menu.getPrefixItemCount();
			int experienceLevel = this.menu.player.experienceLevel;

			for (int l = this.prefixEnchantmentsScrollPosition; l < Math.min(this.prefixEnchantmentsScrollPosition + 4, current_prefix_enchantments.size()); l++) {
				if (this.isHovering(8, 59 + index * 19, 108, 19, mouseX, mouseY)) {
					MutablePair<Holder.Reference<Enchantment>, Integer> entry = current_prefix_enchantments.get(l);
					List<Component> list = new ArrayList<>();
					list.add(Enchantment.getFullname(entry.getLeft(), entry.getRight()));
					Optional<ResourceKey<Enchantment>> optionalRegistryKey = entry.getLeft().unwrapKey();
					if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
						list.add(Component.translatable("enchantment." + optionalRegistryKey.get().identifier().toLanguageKey() + ".desc").withStyle(ChatFormatting.GRAY));
					}
					if (!bl) {
						list.add(CommonComponents.EMPTY);
						int experience_cost_amount = this.menu.existing_enchantment_costs[0] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxCost(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
						int item_cost_amount = this.menu.existing_enchantment_costs[1] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (item_cost_amount > 0) {
							MutableComponent mutableText = Component.literal(item_cost_amount + " ").append(BuiltInRegistries.ITEM.getValue(RPGEnchanting.SERVER_CONFIG.prefix_item_cost.get()).asItem().getName());
							list.add(mutableText.withStyle(prefixItemCount >= item_cost_amount ? ChatFormatting.GRAY : ChatFormatting.RED));
						}

						if (experience_cost_amount > 0) {
							MutableComponent mutableText2;
							if (experience_cost_amount == 1) {
								mutableText2 = Component.translatable("container.enchant.level.one");
							} else {
								mutableText2 = Component.translatable("container.enchant.level.many", new Object[]{experience_cost_amount});
							}
							list.add(mutableText2.withStyle(experienceLevel >= experience_cost_amount ? ChatFormatting.GRAY : ChatFormatting.RED));
						}
					}

					context.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
					return;
				}
				index++;
			}
		}
		if (!current_suffix_enchantments.isEmpty()) {
			int index = 0;
			int suffixItemCount = this.menu.getSuffixItemCount();
			int experienceLevel = this.menu.player.experienceLevel;

			for (int l = this.suffixEnchantmentsScrollPosition; l < Math.min(this.suffixEnchantmentsScrollPosition + 4, current_suffix_enchantments.size()); l++) {
				if (this.isHovering(168, 59 + index * 19, 108, 19, mouseX, mouseY)) {
					MutablePair<Holder.Reference<Enchantment>, Integer> entry = current_suffix_enchantments.get(l);
					List<Component> list = new ArrayList<>();
					list.add(Enchantment.getFullname(entry.getLeft(), entry.getRight()));
					Optional<ResourceKey<Enchantment>> optionalRegistryKey = entry.getLeft().unwrapKey();
					if (optionalRegistryKey.isPresent() && clientConfig.show_enchantment_descriptions.get()) {
						list.add(Component.translatable("enchantment." + optionalRegistryKey.get().identifier().toLanguageKey() + ".desc").withStyle(ChatFormatting.GRAY));
					}
					if (!bl) {
						list.add(CommonComponents.EMPTY);
						int experience_cost_amount = this.menu.existing_enchantment_costs[2] + (int) Math.max(0, Math.floor(entry.getLeft().value().getMaxCost(entry.getRight()) * serverConfig.new_enchantment_exp_cost_multiplier.get()));
						int item_cost_amount = this.menu.existing_enchantment_costs[3] + (int) Math.max(0, Math.floor(entry.getLeft().value().getAnvilCost() * entry.getRight() * serverConfig.new_enchantment_item_cost_multiplier.get()));

						if (item_cost_amount > 0) {
							MutableComponent mutableText = Component.literal(item_cost_amount + " ").append(BuiltInRegistries.ITEM.getValue(RPGEnchanting.SERVER_CONFIG.suffix_item_cost.get()).asItem().getName());
							list.add(mutableText.withStyle(suffixItemCount >= item_cost_amount ? ChatFormatting.GRAY : ChatFormatting.RED));
						}

						if (experience_cost_amount > 0) {
							MutableComponent mutableText2;
							if (experience_cost_amount == 1) {
								mutableText2 = Component.translatable("container.enchant.level.one");
							} else {
								mutableText2 = Component.translatable("container.enchant.level.many", new Object[]{experience_cost_amount});
							}
							list.add(mutableText2.withStyle(experienceLevel >= experience_cost_amount ? ChatFormatting.GRAY : ChatFormatting.RED));
						}
					}

					context.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
					return;
				}
				index++;
			}
		}
	}

	public void doTick() {
		ItemStack itemStack = this.menu.getSlot(0).getItem();
		if (!ItemStack.matches(itemStack, this.stack)) {
			this.stack = itemStack;

			do {
				this.approximatePageAngle = this.approximatePageAngle + (this.random.nextInt(4) - this.random.nextInt(4));
			} while (this.nextPageAngle <= this.approximatePageAngle + 1.0F && this.nextPageAngle >= this.approximatePageAngle - 1.0F);
		}

		this.pageAngle = this.nextPageAngle;
		this.pageTurningSpeed = this.nextPageTurningSpeed;

		if (this.menu.existing_prefix_enchantment != null || this.menu.existing_suffix_enchantment != null) {
			this.nextPageTurningSpeed += 0.2F;
		} else {
			this.nextPageTurningSpeed -= 0.2F;
		}

		this.nextPageTurningSpeed = Mth.clamp(this.nextPageTurningSpeed, 0.0F, 1.0F);
		float f = (this.approximatePageAngle - this.nextPageAngle) * 0.4F;
		float g = 0.2F;
		f = Mth.clamp(f, -0.2F, 0.2F);
		this.pageRotationSpeed = this.pageRotationSpeed + (f - this.pageRotationSpeed) * 0.9F;
		this.nextPageAngle = this.nextPageAngle + this.pageRotationSpeed;
	}
}
