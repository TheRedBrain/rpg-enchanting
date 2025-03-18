package com.github.theredbrain.rpgenchanting.config;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.entry.Entry;
import me.fzzyhmstrs.fzzy_config.util.Walkable;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;

import java.util.HashMap;
import java.util.List;

public class ServerConfig extends Config {

	public ServerConfig() {
		super(RPGEnchanting.identifier("server"));
	}

	public ValidatedInt rpg_enchanting_table_block_reach_radius = new ValidatedInt(3);
//	public ValidatedInt default_hand_crafting_level = new ValidatedInt(0);
//	public ValidatedBoolean show_locked_recipes_in_recipe_list = new ValidatedBoolean(true);
//	public ValidatedBoolean show_locked_recipes_in_crafting_screens = new ValidatedBoolean(false);
//	public ValidatedBoolean show_all_unlocked_special_recipes = new ValidatedBoolean(false);
//	public ValidatedBoolean is_crafting_list_screen_hotkey_enabled = new ValidatedBoolean(true);
//	public ValidatedBoolean combine_advancement_provided_levels = new ValidatedBoolean(true);
//	public ValidatedBoolean combine_block_provided_levels_for_duplicate_blocks = new ValidatedBoolean(true);
//	public ValidatedBoolean combine_block_provided_levels_for_different_blocks = new ValidatedBoolean(true);
	public ValidatedBoolean enable_alternative_item_name_for_enchanted_loot = new ValidatedBoolean(true);
	public ValidatedBoolean hide_normal_enchantment_tooltip_for_enchanted_loot = new ValidatedBoolean(true);
	public ValidatedBoolean enable_item_cost = new ValidatedBoolean(true);

	public ValidatedList<UnlockedEnchantment> enchantments_unlocked_by_advancements = new ValidatedList<>(List.of(
			new UnlockedEnchantment("minecraft:story/smelt_iron", "minecraft:efficiency", 1)
	), new ValidatedAny<>(new UnlockedEnchantment()));

	public ValidatedList<UnlockedEnchantment> enchantments_unlocked_by_blocks = new ValidatedList<>(List.of(
			new UnlockedEnchantment("minecraft:iron_block", "minecraft:silk_touch", 1),
			new UnlockedEnchantment("minecraft:gold_block", "minecraft:fortune", 1),
			new UnlockedEnchantment("minecraft:diamond_block", "minecraft:looting", 2)
	), new ValidatedAny<>(new UnlockedEnchantment()));

	public static class UnlockedEnchantment implements Walkable {

		public UnlockedEnchantment() {
			new UnlockedEnchantment("", "", 0);
		}

		public UnlockedEnchantment(String identifier, String enchantment, int level) {
			this.identifier = identifier;
			this.enchantment = enchantment;
			this.level = level;
		}

		public String identifier;
		public String enchantment;
		public int level;

		public String toString() {
			return "identifier: " + this.identifier +
					", enchantment: " + this.enchantment +
					", level: " + this.level;
		}
	}
}
