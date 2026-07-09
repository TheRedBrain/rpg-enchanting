package com.github.theredbrain.rpgenchanting.config;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import com.github.theredbrain.rpgenchanting.block.RPGEnchantingTableBlock;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.util.Walkable;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedAny;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.List;

public class ServerConfig extends Config {

	public ServerConfig() {
		super(RPGEnchanting.identifier("server"));
	}

	public ValidatedInt rpg_enchanting_table_block_reach_radius = new ValidatedInt(3);
	public ValidatedDouble new_enchantment_exp_cost_multiplier = new ValidatedDouble(0.1);
	public ValidatedDouble old_enchantment_exp_cost_multiplier = new ValidatedDouble(0.1);
	public ValidatedDouble new_enchantment_item_cost_multiplier = new ValidatedDouble(1.0);
	public ValidatedDouble old_enchantment_item_cost_multiplier = new ValidatedDouble(1.0);

	public ValidatedIdentifier prefix_item_cost = ValidatedIdentifier.ofRegistry(Identifier.parse("minecraft:lapis_lazuli"), BuiltInRegistries.ITEM);
	public ValidatedIdentifier suffix_item_cost = ValidatedIdentifier.ofRegistry(Identifier.parse("minecraft:lapis_lazuli"), BuiltInRegistries.ITEM);
	public ValidatedBoolean enable_alternative_item_name_for_enchanted_loot = new ValidatedBoolean(true);
	public ValidatedBoolean hide_normal_enchantment_tooltip_for_enchanted_loot = new ValidatedBoolean(true);

	public ValidatedBoolean enable_enchantment_unlocking_by_chiseled_bookshelves = new ValidatedBoolean(true);
	public ValidatedEnum<RPGEnchantingTableBlock.EnchantmentUnlockMode> default_enchantment_unlock_mode = new ValidatedEnum<>(RPGEnchantingTableBlock.EnchantmentUnlockMode.ADDITION);
	public ValidatedEnum<RPGEnchantingTableBlock.BookCost> default_book_cost = new ValidatedEnum<>(RPGEnchantingTableBlock.BookCost.PARTIAL_CONSUME);
	public ValidatedIdentifier enchanted_book_replacement = ValidatedIdentifier.ofRegistry(Identifier.parse("book"), BuiltInRegistries.ITEM);
	public ValidatedBoolean enable_ambient_enchant_particles = new ValidatedBoolean(true);
	public ValidatedInt ambient_enchant_particle_radius = new ValidatedInt(3);

	public ValidatedBoolean enable_enchanted_by_player_component_application = new ValidatedBoolean(true);

	public ValidatedList<UnlockedEnchantment> enchantments_unlocked_by_advancements = new ValidatedList<>(List.of(
	), new ValidatedAny<>(new UnlockedEnchantment()));

	public ValidatedList<UnlockedEnchantment> enchantments_unlocked_by_blocks = new ValidatedList<>(List.of(
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
