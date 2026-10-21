import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Build-time exporter only. Run with Gson and SnakeYAML on the Java 21 classpath. */
public class ExportEquipmentAttributes {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ExportEquipmentAttributes.java attribute-display.yml output.json");
        }
        Map<?, ?> root;
        try (var reader = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)) {
            root = new Yaml().load(reader);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<?, ?> globalRoll = section(root.get("roll-percent"));
        for (var entry : section(root.get("attributes")).entrySet()) {
            Map<?, ?> value = section(entry.getValue());
            List<?> names = value.get("names") instanceof List<?> list ? list : List.of();
            Map<String, Object> display = new LinkedHashMap<>();
            display.put("name", names.isEmpty() ? string(value, "name", entry.getKey().toString()) : names.getFirst());
            display.put("icon", string(value, "icon", ""));
            display.put("suffix", string(value, "suffix", ""));
            var hex = Pattern.compile("#([0-9a-fA-F]{6})").matcher(string(value, "color", ""));
            display.put("color", hex.find() ? Integer.parseInt(hex.group(1), 16) : 0xAAAAAA);
            display.put("percent", Boolean.TRUE.equals(value.get("percent")));
            display.put("lowerIsBetter", string(section(value.get("roll-percent")), "direction",
                    string(globalRoll, "direction", "higher")).equalsIgnoreCase("lower"));
            result.put(entry.getKey().toString(), display);
        }
        Files.writeString(Path.of(args[1]), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(result) + "\n", StandardCharsets.UTF_8);
    }

    private static Map<?, ?> section(Object object) {
        return object instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value.toString();
    }
}
