package com.github.theredbrain.rpgenchanting.mixin.enchantment;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

	@ModifyReturnValue(
			method = "enchant(Lnet/minecraft/util/math/random/Random;Lnet/minecraft/item/ItemStack;ILjava/util/stream/Stream;)Lnet/minecraft/item/ItemStack;",
			at = @At("RETURN")
	)
	private static ItemStack rpgenchanting$enchant(ItemStack original) {
		if (RPGEnchanting.SERVER_CONFIG.enable_alternative_item_name_for_enchanted_loot.get()) {
			original.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
		}
		if (RPGEnchanting.SERVER_CONFIG.hide_normal_enchantment_tooltip_for_enchanted_loot.get() && !original.isOf(Items.BOOK)) {
			TooltipDisplayComponent tooltipDisplayComponent = original.getOrDefault(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplayComponent.DEFAULT);
			tooltipDisplayComponent.with(DataComponentTypes.ENCHANTMENTS, true);
			original.set(DataComponentTypes.TOOLTIP_DISPLAY, tooltipDisplayComponent);
		}
		return original;
	}

}
