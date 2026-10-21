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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class ItemSlotTintRenderer {
    private static final Identifier TEXTURE = Identifier.of("rapidsutils", "textures/gui/item_slot_tint.png");
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

    public static void draw(DrawContext context, Slot slot) {
        RapidsConfig config = RapidsConfig.get();
        if (!config.itemSlotTint || config.itemSlotTintOpacity <= 0 || !slot.hasStack()) {
            return;
        }
        OptionalInt color = color(slot.getStack());
        if (color.isEmpty()) {
            return;
        }
        int argb = (Math.round(config.itemSlotTintOpacity * 255) << 24) | color.getAsInt();
        // One textured quad per slot; the mask provides the darkening and soft circular edge.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, slot.x + 2, slot.y + 2,
                0F, 0F, 12, 12, 32, 32, 32, 32, argb);
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
