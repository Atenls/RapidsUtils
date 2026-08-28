package com.atenls.rapidsutils.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RapidsUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "rapidsutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("RapidsUtils client initialized");
    }
}
