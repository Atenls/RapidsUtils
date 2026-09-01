package com.atenls.rapidsutils.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.Optional;

public final class PlayerVitalsParser {
    private PlayerVitalsParser() {
    }

    public static Optional<PlayerVitals> parse(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }

            JsonObject root = parsed.getAsJsonObject();
            BigDecimal health = number(root.get("health"));
            BigDecimal healthMax = number(root.get("health_max"));
            BigDecimal healthRegen = number(root.get("health_regen"));
            BigDecimal mana = number(root.get("mana"));
            BigDecimal manaMax = number(root.get("mana_max"));
            BigDecimal manaRegen = number(root.get("mana_regen"));
            if (health == null || healthMax == null || healthRegen == null
                    || mana == null || manaMax == null || manaRegen == null) {
                return Optional.empty();
            }
            return Optional.of(new PlayerVitals(
                    health, healthMax, healthRegen, mana, manaMax, manaRegen
            ));
        } catch (JsonParseException | NumberFormatException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    private static BigDecimal number(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return new BigDecimal(element.getAsString());
    }
}
