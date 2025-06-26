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

import java.util.Objects;
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
			String prefixEnchantmentString = "";
			String suffixEnchantmentString = "";
			String prefixEnchantmentTranslationKey = "";
			String suffixEnchantmentTranslationKey = "";
			int prefixEnchantmentLevel = 0;
			int suffixEnchantmentLevel = 0;
			if (itemEnchantmentsComponent != null) {
				for (RegistryEntry<Enchantment> enchantmentEntry : itemEnchantmentsComponent.getEnchantments()) {
					Optional<RegistryKey<Enchantment>> optional = enchantmentEntry.getKey();
					if (optional.isPresent()) {
						Identifier id = optional.get().getValue();
						if (enchantmentEntry.isIn(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							prefixEnchantmentTranslationKey = id.toTranslationKey();
							prefixEnchantmentLevel = itemEnchantmentsComponent.getLevel(enchantmentEntry);
							prefixEnchantmentString = RPGEnchanting.MOD_ID + "." + prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + ".prefix";
						}
						if (enchantmentEntry.isIn(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							suffixEnchantmentTranslationKey = id.toTranslationKey();
							suffixEnchantmentLevel = itemEnchantmentsComponent.getLevel(enchantmentEntry);
							suffixEnchantmentString = RPGEnchanting.MOD_ID + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel + ".suffix";
						}
					}
				}
			}

			Text text = instance.get(DataComponentTypes.ITEM_NAME);
			if (text == null) {

				// checks if the combination of item and enchantments has a dedicated translation
				// this is disabled when the item has a custom name
				String translationKeyOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + instance.getItem().getTranslationKey() + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel;
				MutableText textOverwrite = Text.translatable(translationKeyOverwrite);
				RPGEnchanting.info("textOverwrite.getString(): " + textOverwrite.getString());
				RPGEnchanting.info("translationKeyOverwrite: " + translationKeyOverwrite);
				if (!Objects.equals(textOverwrite.getString(), translationKeyOverwrite)) {
					return textOverwrite;
				}

				text = instance.getItem().getName(instance);
			}
			if (!suffixEnchantmentString.isEmpty()) {
				text = Text.translatable(suffixEnchantmentString, text);
			}
			if (!prefixEnchantmentString.isEmpty()) {
				text = Text.translatable(prefixEnchantmentString, text);
			}
			return text;
		} else {
			return original.call(instance, componentType);
		}
	}
}
