package com.github.theredbrain.rpgenchanting.mixin.loot.function;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyLootFunctionMixin {

	@ModifyReturnValue(
			method = "enchantItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;",
			at = @At("RETURN")
	)
	private static ItemStack rpgenchanting$addEnchantmentToStack(ItemStack original) {
		if (RPGEnchanting.SERVER_CONFIG.enable_alternative_item_name_for_enchanted_loot.get() && !original.is(Items.BOOK)) {
			original.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
		}
		if (RPGEnchanting.SERVER_CONFIG.hide_normal_enchantment_tooltip_for_enchanted_loot.get() && !original.is(Items.BOOK)) {
			TooltipDisplay tooltipDisplayComponent = original.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
			tooltipDisplayComponent.withHidden(DataComponents.ENCHANTMENTS, true);
			original.set(DataComponents.TOOLTIP_DISPLAY, tooltipDisplayComponent);
		}

		return original;
	}

}
