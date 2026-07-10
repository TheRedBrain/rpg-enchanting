package com.github.theredbrain.rpgenchanting.mixin.item;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.item.EnchantedItemNameHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@WrapOperation(
			method = "getHoverName",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItemName()Lnet/minecraft/network/chat/Component;")
	)
	private Component rpgenchanting$wrap_getName(ItemStack instance, Operation<Component> original) {
		if (instance.has(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS)) {
			return EnchantedItemNameHelper.getCustomEnchantedItemName(instance);
		} else {
			return original.call(instance);
		}
	}
}
