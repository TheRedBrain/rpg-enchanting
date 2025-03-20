package com.github.theredbrain.rpgenchanting.config;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;

public class ClientConfig extends Config {

	public ClientConfig() {
		super(RPGEnchanting.identifier("client"));
	}

	public ValidatedBoolean show_enchantment_descriptions = new ValidatedBoolean(false);
	public ValidatedBoolean show_inactive_slots = new ValidatedBoolean(false);
	public ValidatedBoolean enable_texture_cycling_for_item_cost_slot = new ValidatedBoolean(false);

}

