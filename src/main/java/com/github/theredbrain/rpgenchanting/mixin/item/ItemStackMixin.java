package com.github.theredbrain.rpgenchanting.mixin.item;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@WrapOperation(
			method = "getName",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;", ordinal = 1)
	)
	private Object rpgenchanting$wrap_getName(ItemStack instance, ComponentType<?> componentType, Operation<Object> original) {
		if (instance.contains(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS)) {
			ItemEnchantmentsComponent itemEnchantmentsComponent = instance.get(DataComponentTypes.ENCHANTMENTS);
			MutableText prefixEnchantmentText = Text.empty();
			MutableText suffixEnchantmentText = Text.empty();
			if (itemEnchantmentsComponent != null) {
				for (RegistryEntry<Enchantment> enchantmentEntry : itemEnchantmentsComponent.getEnchantments()) {
					Optional<RegistryKey<Enchantment>> optional = enchantmentEntry.getKey();
					if (optional.isPresent()) {
						Identifier id = optional.get().getValue();
						if (enchantmentEntry.isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							prefixEnchantmentText = Text.translatable("rpg_enchanting." + id.toTranslationKey() + "." + itemEnchantmentsComponent.getLevel(enchantmentEntry) + ".prefix");
						}
						if (enchantmentEntry.isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							suffixEnchantmentText = Text.translatable("rpg_enchanting." + id.toTranslationKey() + "." + itemEnchantmentsComponent.getLevel(enchantmentEntry) + ".suffix");
						}
					}
				}
			}
			Text nameText = instance.get(DataComponentTypes.ITEM_NAME);
			if (nameText == null) {
				nameText = instance.getItem().getName(instance);
			}
			return prefixEnchantmentText.append(nameText).append(suffixEnchantmentText);
		} else {
			return original.call(instance, componentType);
		}
	}
}
