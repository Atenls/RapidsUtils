package com.atenls.rapidsutils.client;

import com.atenls.rapidsutils.client.network.RapidsDataReceiver;
import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.client.render.RapidsHudRenderer;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RapidsUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "rapidsutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        RapidsConfig config = RapidsConfig.load();
        TopicSnapshotStore store = new TopicSnapshotStore();
        RapidsDataReceiver.register(store);
        RapidsHudRenderer renderer = new RapidsHudRenderer(store, config);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "data_hud"),
                renderer::render
        );
        LOGGER.info("RapidsUtils client initialized");
    }
}
