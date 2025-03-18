package com.github.theredbrain.rpgenchanting.mixin.loot.function;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantRandomlyLootFunction.class)
public class EnchantRandomlyLootFunctionMixin {

	@ModifyReturnValue(
			method = "addEnchantmentToStack(Lnet/minecraft/item/ItemStack;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/util/math/random/Random;)Lnet/minecraft/item/ItemStack;",
			at = @At("RETURN")
	)
	private static ItemStack rpgenchanting$addEnchantmentToStack(ItemStack original) {
		if (RPGEnchanting.SERVER_CONFIG.enable_alternative_item_name_for_enchanted_loot.get() && !original.isOf(Items.BOOK)) {
			original.set(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS, Unit.INSTANCE);
		}
		if (RPGEnchanting.SERVER_CONFIG.hide_normal_enchantment_tooltip_for_enchanted_loot.get() && !original.isOf(Items.BOOK)) {
			ItemEnchantmentsComponent itemEnchantmentsComponent = original.getEnchantments();
			if (itemEnchantmentsComponent != null) {
				ItemEnchantmentsComponent newItemEnchantmentsComponent = new ItemEnchantmentsComponent.Builder(itemEnchantmentsComponent).build().withShowInTooltip(false);
				original.set(DataComponentTypes.ENCHANTMENTS, newItemEnchantmentsComponent);
			}
		}

		return original;
	}

}
