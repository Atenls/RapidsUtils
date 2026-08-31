package com.atenls.rapidsutils.client.config;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RapidsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rapidsutils.json");
    public static final double DEFAULT_DISPLAY_SECONDS = 3.0D;
    public static final int DEFAULT_TOPIC_INDEX = 10;
    public static final String DISPLAY_TEMPLATE = "{display}{extraData}";
    public static final String HIDDEN_TOPIC_TEMPLATE = "{display}";
    private static final String HIDDEN_TOPIC_PREFIX = "hidden_";
    private static final Set<String> HIDDEN_TOPICS = Set.of("dungeon", "mastery", "update", "reload");
    private static final Map<String, TopicSettings> BUILT_IN_TOPICS = builtInTopics();

    private static RapidsConfig loaded;

    public boolean enabled = true;
    public int margin = 5;
    public float backgroundOpacity = 0.6F;
    public int maxWidth = 300;
    public Map<String, TopicSettings> topics = new LinkedHashMap<>();

    public static RapidsConfig load() {
        if (loaded != null) {
            return loaded;
        }
        if (!Files.exists(PATH)) {
            RapidsConfig config = new RapidsConfig();
            config.save();
            loaded = config;
            return loaded;
        }

        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            RapidsConfig config = GSON.fromJson(reader, RapidsConfig.class);
            if (config == null) {
                config = new RapidsConfig();
            }
            config.sanitize();
            loaded = config;
            return loaded;
        } catch (IOException | JsonSyntaxException e) {
            RapidsUtilsClient.LOGGER.warn("Failed to load rapidsutils.json; using defaults", e);
            loaded = new RapidsConfig();
            return loaded;
        }
    }

    public static RapidsConfig get() {
        return load();
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            RapidsUtilsClient.LOGGER.warn("Failed to write default rapidsutils.json", e);
        }
    }

    public TopicSettings topic(String topic) {
        TopicSettings builtIn = BUILT_IN_TOPICS.get(topic);
        return builtIn == null ? topics.get(topic) : builtIn;
    }

    public double displaySeconds(String topic) {
        TopicSettings settings = topic(topic);
        return settings == null ? DEFAULT_DISPLAY_SECONDS : settings.displaySeconds;
    }

    public int index(String topic) {
        TopicSettings settings = topic(topic);
        return settings == null ? DEFAULT_TOPIC_INDEX : settings.index;
    }

    public void putTopic(String name, TopicSettings settings) {
        topics.put(name, settings);
        sanitize();
    }

    public void removeTopic(String name) {
        topics.remove(name);
    }

    private void sanitize() {
        margin = Math.max(0, Math.min(64, margin));
        backgroundOpacity = Math.max(0.15F, Math.min(0.95F, backgroundOpacity));
        maxWidth = Math.max(140, Math.min(600, maxWidth));
        if (topics == null) {
            topics = new LinkedHashMap<>();
        } else if (!(topics instanceof LinkedHashMap)) {
            topics = new LinkedHashMap<>(topics);
        }
        topics.entrySet().removeIf(entry -> entry.getKey() == null
                || entry.getKey().isBlank()
                || isHiddenTopic(entry.getKey())
                || entry.getValue() == null);
        topics.forEach((topic, settings) -> {
            settings.sanitize(DEFAULT_TOPIC_INDEX);
        });
    }

    private static Map<String, TopicSettings> builtInTopics() {
        LinkedHashMap<String, TopicSettings> defaults = new LinkedHashMap<>();
        defaults.put("dungeon", new TopicSettings(DEFAULT_DISPLAY_SECONDS, 8, DISPLAY_TEMPLATE));
        defaults.put("mastery", new TopicSettings(DEFAULT_DISPLAY_SECONDS, 5, DISPLAY_TEMPLATE));
        defaults.put("update", new TopicSettings(60.0D, 20, DISPLAY_TEMPLATE));
        defaults.put("reload", new TopicSettings(60.0D, 1, DISPLAY_TEMPLATE));
        return Collections.unmodifiableMap(defaults);
    }

    public static TopicSettings defaultTopic(String topic) {
        TopicSettings settings = BUILT_IN_TOPICS.get(topic);
        return settings == null ? new TopicSettings(DEFAULT_DISPLAY_SECONDS, DEFAULT_TOPIC_INDEX, "") : settings;
    }

    public static boolean isHiddenTopic(String topic) {
        return HIDDEN_TOPICS.contains(topic) || topic.startsWith(HIDDEN_TOPIC_PREFIX);
    }

    public static String hiddenTemplate(String topic) {
        if (HIDDEN_TOPICS.contains(topic)) {
            return DISPLAY_TEMPLATE;
        }
        return topic.startsWith(HIDDEN_TOPIC_PREFIX) ? HIDDEN_TOPIC_TEMPLATE : null;
    }

    public static final class TopicSettings {
        public double displaySeconds = DEFAULT_DISPLAY_SECONDS;
        public Integer index;
        public String template = "";

        public TopicSettings() {
        }

        public TopicSettings(double displaySeconds, int index, String template) {
            this.displaySeconds = displaySeconds;
            this.index = index;
            this.template = template;
            sanitize(index);
        }

        private void sanitize(int fallbackIndex) {
            displaySeconds = Math.max(0.5D, Math.min(600.0D, displaySeconds));
            if (index == null) {
                index = fallbackIndex;
            }
            if (template == null) {
                template = "";
            } else if (template.length() > 8192) {
                template = template.substring(0, 8192);
            }
        }
    }
}
