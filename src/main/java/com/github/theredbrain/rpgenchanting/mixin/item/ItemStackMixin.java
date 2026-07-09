package com.github.theredbrain.rpgenchanting.mixin.item;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@WrapOperation(
			method = "getHoverName",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItemName()Lnet/minecraft/network/chat/Component;")
	)
	private Component rpgenchanting$wrap_getName(ItemStack instance, Operation<Component> original) {
		if (instance.has(RPGEnchanting.SHOW_ENCHANTMENT_NAME_ADDITIONS)) {
			ItemEnchantments itemEnchantmentsComponent = instance.get(DataComponents.ENCHANTMENTS);
			String prefixEnchantmentString = "";
			String suffixEnchantmentString = "";
			String prefixEnchantmentTranslationKey = "";
			String suffixEnchantmentTranslationKey = "";
			int prefixEnchantmentLevel = 0;
			int suffixEnchantmentLevel = 0;
			if (itemEnchantmentsComponent != null) {
				for (Holder<Enchantment> enchantmentEntry : itemEnchantmentsComponent.keySet()) {
					Optional<ResourceKey<Enchantment>> optional = enchantmentEntry.unwrapKey();
					if (optional.isPresent()) {
						Identifier id = optional.get().identifier();
						if (enchantmentEntry.is(RPGEnchanting.PREFIX_ENCHANTMENTS)) {
							prefixEnchantmentTranslationKey = id.toLanguageKey();
							prefixEnchantmentLevel = itemEnchantmentsComponent.getLevel(enchantmentEntry);
							prefixEnchantmentString = RPGEnchanting.MOD_ID + "." + prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + ".prefix";
						}
						if (enchantmentEntry.is(RPGEnchanting.SUFFIX_ENCHANTMENTS)) {
							suffixEnchantmentTranslationKey = id.toLanguageKey();
							suffixEnchantmentLevel = itemEnchantmentsComponent.getLevel(enchantmentEntry);
							suffixEnchantmentString = RPGEnchanting.MOD_ID + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel + ".suffix";
						}
					}
				}
			}

			Component text = instance.getCustomName();
//			MutableText textPrefixOverwrite = Text.empty();
//			MutableText textSuffixOverwrite = Text.empty();
			if (text == null) {

				// checks if the combination of item and enchantments has a dedicated translation
				// this is disabled when the item has a custom name
				String translationKeyOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + instance.getItem().getDescriptionId() + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel;
				MutableComponent textOverwrite = Component.translatable(translationKeyOverwrite);
//				RPGEnchanting.info("textOverwrite.getString(): " + textOverwrite.getString());
//				RPGEnchanting.info("translationKeyOverwrite: " + translationKeyOverwrite);
				if (!Objects.equals(textOverwrite.getString(), translationKeyOverwrite)) {
					return textOverwrite;
				}

//				// prefix overwrite
//				String translationKeyPrefixOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + instance.getItem().getTranslationKey();
//				textPrefixOverwrite = Text.translatable(translationKeyPrefixOverwrite);
//				RPGEnchanting.info("textPrefixOverwrite.getString(): " + textPrefixOverwrite.getString());
//				RPGEnchanting.info("translationKeyPrefixOverwrite: " + translationKeyPrefixOverwrite);
//				if (Objects.equals(textPrefixOverwrite.getString().hashCode(), translationKeyPrefixOverwrite.hashCode())) {
//					translationKeyPrefixOverwrite = "";
//					textPrefixOverwrite = Text.empty();
//				}
//
//				// suffix overwrite
//				String translationKeySuffixOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + instance.getItem().getTranslationKey();
//				textSuffixOverwrite = Text.translatable(translationKeySuffixOverwrite);
//				RPGEnchanting.info("textSuffixOverwrite.getString(): " + textSuffixOverwrite.getString());
//				RPGEnchanting.info("translationKeySuffixOverwrite: " + translationKeySuffixOverwrite);
//				if (Objects.equals(textSuffixOverwrite.getString(), translationKeySuffixOverwrite)) {
//					translationKeySuffixOverwrite = "";
//					textSuffixOverwrite = Text.empty();
//				}

				text = instance.getItemName();
			}
			if (!suffixEnchantmentString.isEmpty()) {
				text = Component.translatable(suffixEnchantmentString, text);
			}
			if (!prefixEnchantmentString.isEmpty()) {
//				if () {
//				}
				text = Component.translatable(prefixEnchantmentString, text);
			}
			return text;
		} else {
			return original.call(instance);
		}
	}
}
