package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.protocol.PayloadData;
import com.atenls.rapidsutils.text.MinecraftColorParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TopicTemplateFormatter {
    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z0-9_.-]+)}");
    private static final int MAX_VALUE_CHARACTERS = 240;
    private static final Map<String, String> BUILT_INS = Map.of("rhombus", "◆");

    public List<HudLine> format(String template, PayloadData data) {
        Map<String, String> variables = new LinkedHashMap<>(BUILT_INS);
        collectVariables("", data, variables, 0);

        ArrayList<HudLine> lines = new ArrayList<>();
        for (String line : template.split("\\R", -1)) {
            String resolved = resolve(line, variables);
            lines.add(new HudLine(0, MinecraftColorParser.parse(
                    resolved,
                    JsonDisplayFormatter.PRIMARY_TEXT,
                    MAX_VALUE_CHARACTERS
            )));
        }
        return List.copyOf(lines);
    }

    private static String resolve(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = variables.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static void collectVariables(String prefix, PayloadData value, Map<String, String> variables, int depth) {
        if (depth > 8) {
            return;
        }
        if (value instanceof PayloadData.ObjectValue object) {
            for (Map.Entry<String, PayloadData> entry : object.values().entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                PayloadData child = entry.getValue();
                variables.put(key, compactValue(child));
                collectVariables(key, child, variables, depth + 1);
            }
        }
    }

    private static String compactValue(PayloadData value) {
        if (value instanceof PayloadData.ScalarValue scalar) {
            return scalar.value();
        }
        if (value instanceof PayloadData.ArrayValue array) {
            return array.values().stream()
                    .limit(12)
                    .map(TopicTemplateFormatter::compactValue)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        return ((PayloadData.ObjectValue) value).values().entrySet().stream()
                .limit(12)
                .map(entry -> entry.getKey() + ": " + compactValue(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
