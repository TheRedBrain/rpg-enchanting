package com.github.theredbrain.rpgenchanting.item;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Objects;
import java.util.Optional;

public class EnchantedItemNameHelper {

	public static Component getCustomEnchantedItemName(ItemStack itemStack) {
		ItemEnchantments itemEnchantmentsComponent = itemStack.get(DataComponents.ENCHANTMENTS);
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

		Component text = itemStack.getCustomName();
		MutableComponent textPrefixOverwrite = Component.empty();
		MutableComponent textSuffixOverwrite = Component.empty();
		String translationKeyPrefixOverwrite = "";
		boolean usePrefixOverwrite = false;
		boolean useSuffixOverwrite = false;
		if (text == null) {

			// checks if the combination of item and enchantments has a dedicated translation
			// this is disabled when the item has a custom name
			String translationKeyOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + itemStack.getItem().getDescriptionId() + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel;
			MutableComponent textOverwrite = Component.translatable(translationKeyOverwrite);
//			RPGEnchanting.info("textOverwrite.getString(): " + textOverwrite.getString());
//			RPGEnchanting.info("translationKeyOverwrite: " + translationKeyOverwrite);
			if (!Objects.equals(textOverwrite.getString(), translationKeyOverwrite)) {
				return textOverwrite;
			}

			// suffix overwrite
			String translationKeySuffixOverwrite = itemStack.getItem().getDescriptionId() + "." + suffixEnchantmentTranslationKey + "." + suffixEnchantmentLevel;
			textSuffixOverwrite = Component.translatable(translationKeySuffixOverwrite);
//			RPGEnchanting.info("textSuffixOverwrite.getString(): " + textSuffixOverwrite.getString());
//			RPGEnchanting.info("translationKeySuffixOverwrite: " + translationKeySuffixOverwrite);
			if (Objects.equals(textSuffixOverwrite.getString(), translationKeySuffixOverwrite)) {
				translationKeySuffixOverwrite = "";
				textSuffixOverwrite = Component.empty();
			} else {
				useSuffixOverwrite = true;
			}

			if (!useSuffixOverwrite) {
				// prefix overwrite
				translationKeyPrefixOverwrite = prefixEnchantmentTranslationKey + "." + prefixEnchantmentLevel + "." + itemStack.getItem().getDescriptionId();
				textPrefixOverwrite = Component.translatable(translationKeyPrefixOverwrite);
//				RPGEnchanting.info("textPrefixOverwrite.getString(): " + textPrefixOverwrite.getString());
//				RPGEnchanting.info("translationKeyPrefixOverwrite: " + translationKeyPrefixOverwrite);
				if (Objects.equals(textPrefixOverwrite.getString(), translationKeyPrefixOverwrite)) {
					translationKeyPrefixOverwrite = "";
					textPrefixOverwrite = Component.empty();
				} else {
					usePrefixOverwrite = true;
				}
			}

			text = itemStack.getItemName();
		}
		if (!useSuffixOverwrite) {
			if (!suffixEnchantmentString.isEmpty()) {
				text = Component.translatable(suffixEnchantmentString, text);
			}
		} else {
			text = textSuffixOverwrite;
		}

		if (usePrefixOverwrite) {
			text = textPrefixOverwrite;
			if (!suffixEnchantmentString.isEmpty()) {
				text = Component.translatable(suffixEnchantmentString, text);
			}
		} else {
			if (!prefixEnchantmentString.isEmpty()) {
				text = Component.translatable(prefixEnchantmentString, text);
			}
		}
		return text;
	}
}
