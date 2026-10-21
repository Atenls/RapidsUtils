package com.atenls.rapidsutils.client.equipment;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EquipmentAttributeDisplays {
    private static final java.lang.reflect.Type TYPE = new TypeToken<Map<String, Display>>() { }.getType();
    private static final Map<String, Display> DISPLAYS = new LinkedHashMap<>();

    private EquipmentAttributeDisplays() { }

    public static void load() {
        DISPLAYS.clear();
        try (Reader reader = new InputStreamReader(EquipmentAttributeDisplays.class.getResourceAsStream(
                "/assets/rapidsutils/equipment_attributes.json"), StandardCharsets.UTF_8)) {
            read(reader);
        } catch (Exception exception) {
            RapidsUtilsClient.LOGGER.warn("Failed to load bundled equipment attribute displays", exception);
        }
        Path path = FabricLoader.getInstance().getConfigDir().resolve("rapidsutils-equipment-attributes.json");
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                read(reader);
            } catch (Exception exception) {
                RapidsUtilsClient.LOGGER.warn("Failed to load equipment attribute display overrides", exception);
            }
        }
    }

    private static void read(Reader reader) {
        Map<String, Display> values = new Gson().fromJson(reader, TYPE);
        if (values != null) {
            values.forEach((key, value) -> {
                if (key != null && value != null) {
                    DISPLAYS.put(key.toUpperCase(Locale.ROOT), value);
                }
            });
        }
    }

    public static Display get(String key, String fallback) {
        return DISPLAYS.getOrDefault(key, new Display(fallback.isBlank() ? key : fallback,
                "", "", 0xAAAAAA, false, false));
    }

    public record Display(String name, String icon, String suffix, int color, boolean percent, boolean lowerIsBetter) {
        public Display {
            name = name == null ? "" : name;
            icon = icon == null ? "" : icon;
            suffix = suffix == null ? "" : suffix;
        }
    }
}
