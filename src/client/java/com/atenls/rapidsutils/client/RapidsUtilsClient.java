package com.atenls.rapidsutils.client;

import com.atenls.rapidsutils.client.network.RapidsDataReceiver;
import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.client.render.RapidsHudRenderer;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public final class RapidsUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "rapidsutils";
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        RapidsConfig config = RapidsConfig.load();
        AtomicLong clientTicks = new AtomicLong();
        TopicSnapshotStore store = new TopicSnapshotStore(clientTicks::get);
        ClientTickEvents.END_CLIENT_TICK.register(client -> clientTicks.incrementAndGet());
        RapidsDataReceiver.register(store);
        registerKeyBindings(config);
        RapidsHudRenderer renderer = new RapidsHudRenderer(store, config);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "data_hud"),
                renderer::render
        );
        LOGGER.info("RapidsUtils client initialized");
    }

    private static void registerKeyBindings(RapidsConfig config) {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
        KeyBinding toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rapidsutils.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                return;
            }
            while (toggleHud.wasPressed()) {
                config.enabled = !config.enabled;
                config.save();
            }
        });
    }
}
