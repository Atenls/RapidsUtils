package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.equipment.SlotTintColors;
import com.atenls.rapidsutils.text.MinecraftColorParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class ItemSlotTintRenderer {
    private static final Identifier TEXTURE = Identifier.of("rapidsutils", "textures/gui/item_slot_tint.png");
    private static final Identifier CENTER_TEXTURE = Identifier.of("rapidsutils", "textures/gui/item_slot_tint_center.png");
    private static final Map<NbtComponent, SlotTintColors.Resolution> TAG_CACHE = cache();
    private static final Map<Text, OptionalInt> NAME_CACHE = cache();
    private static SlotTintColors colors = SlotTintColors.empty();

    private ItemSlotTintRenderer() { }

    public static void load() {
        clear();
        colors = SlotTintColors.empty();
        try (Reader reader = new InputStreamReader(ItemSlotTintRenderer.class.getResourceAsStream(
                "/assets/rapidsutils/quality_colors.json"), StandardCharsets.UTF_8)) {
            colors = colors.withOverrides(reader);
        } catch (Exception exception) {
            RapidsUtilsClient.LOGGER.warn("Failed to load bundled slot tint colors", exception);
        }
        Path overrides = FabricLoader.getInstance().getConfigDir().resolve("rapidsutils-quality-colors.json");
        if (Files.isRegularFile(overrides)) {
            try (Reader reader = Files.newBufferedReader(overrides, StandardCharsets.UTF_8)) {
                colors = colors.withOverrides(reader);
            } catch (Exception exception) {
                RapidsUtilsClient.LOGGER.warn("Failed to load slot tint color overrides", exception);
            }
        }
    }

    public static void drawSlots(DrawContext context, List<Slot> slots) {
        RapidsConfig config = RapidsConfig.get();
        if (!config.itemSlotTint || config.itemSlotTintOpacity <= 0) {
            return;
        }
        boolean drawn = false;
        for (Slot slot : slots) {
            if (slot.isEnabled() && slot.hasStack()) {
                drawn |= draw(context, slot, config);
            }
        }
        if (drawn) {
            // Keep every item above every tint.
            context.createNewRootLayer();
        }
    }

    private static boolean draw(DrawContext context, Slot slot, RapidsConfig config) {
        OptionalInt color = color(slot.getStack());
        if (color.isEmpty()) {
            return false;
        }
        int argb = (Math.round(config.itemSlotTintOpacity * 255) << 24) | color.getAsInt();
        // Center a 16-pixel circle on the 16-pixel item. The base controls edge opacity;
        // the radial overlay increases opacity to 100% at the center using source-over blending.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, slot.x, slot.y,
                0F, 0F, 16, 16, 32, 32, 32, 32, argb);
        if (config.itemSlotTintOpacity < 1F) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, CENTER_TEXTURE, slot.x, slot.y,
                    0F, 0F, 16, 16, 32, 32, 32, 32, 0xFF000000 | color.getAsInt());
        }
        return true;
    }

    private static OptionalInt color(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return OptionalInt.empty();
        }
        SlotTintColors.Resolution resolution = TAG_CACHE.computeIfAbsent(custom, component -> colors.resolve(
                component.copyNbt().getCompoundOrEmpty("PublicBukkitValues").getString("dpitemmanager:tags", "")));
        if (!resolution.eligible()) {
            return OptionalInt.empty();
        }
        if (resolution.qualityColor().isPresent()) {
            return resolution.qualityColor();
        }
        return NAME_CACHE.computeIfAbsent(stack.getName(), ItemSlotTintRenderer::nameColor);
    }

    private static OptionalInt nameColor(Text name) {
        // Names normally arrive as styled text; also support literal legacy / hex color codes.
        Optional<Integer> first = name.visit((style, text) -> {
            OptionalInt encoded = MinecraftColorParser.firstColor(text);
            if (encoded.isPresent()) {
                return Optional.of(encoded.getAsInt());
            }
            if (!text.isBlank() && style.getColor() != null) {
                return Optional.of(style.getColor().getRgb());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return first.isPresent() ? OptionalInt.of(first.get()) : OptionalInt.empty();
    }

    public static void clear() {
        TAG_CACHE.clear();
        NAME_CACHE.clear();
    }

    private static <K, V> Map<K, V> cache() {
        return new LinkedHashMap<>(256, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > 256;
            }
        };
    }
}
