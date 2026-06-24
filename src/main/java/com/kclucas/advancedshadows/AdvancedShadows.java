package com.kclucas.advancedshadows;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedShadows implements ModInitializer {
	public static final String MOD_ID = "advancedshadows";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("AdvancedShadows by KCLucas loaded.");
	}
}