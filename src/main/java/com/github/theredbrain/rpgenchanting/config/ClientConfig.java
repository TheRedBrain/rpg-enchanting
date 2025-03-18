package com.github.theredbrain.rpgenchanting.config;

import com.github.theredbrain.rpgenchanting.RPGEnchanting;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;

public class ClientConfig extends Config {

	public ClientConfig() {
		super(RPGEnchanting.identifier("client"));
	}

	public ValidatedBoolean show_inactive_slots = new ValidatedBoolean(false);

}

