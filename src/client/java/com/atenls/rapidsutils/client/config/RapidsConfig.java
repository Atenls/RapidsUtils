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
import java.util.LinkedHashMap;
import java.util.Map;

public final class RapidsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rapidsutils.json");
    public static final double DEFAULT_DISPLAY_SECONDS = 3.0D;
    public static final String DUNGEON_TEMPLATE = """
            {rhombus} {dungeonDisplay}
               &#999999特殊掉落 &#80b0d0{itemgot}/{itemgotmax}
               &#999999材料掉落 &#80b0d0{dropsgot}/{dropsgotmax}
               &#999999药剂掉落 &#80b0d0{healingPotionGot}/{healingPotionGotMax}""";
    public static final String MASTERY_TEMPLATE =
            "{rhombus} &#8098b8天赋状态{lootinstinctDisplay}{chestmagnetDisplay}{rarelootDisplay}";
    public static final String RELOAD_TEMPLATE =
            "{reload}";
    public static final String UPDATE_TEMPLATE =
            "{rhombus} &#8098b8Mod 已有可用更新! {version} \n 前往 wiki.dp4.us/#/rapids/updatelogs 查看更新日志并获取新 Mod !";

    private static RapidsConfig loaded;

    public boolean enabled = true;
    public int margin = 5;
    public float backgroundOpacity = 0.6F;
    public int maxWidth = 300;
    public Map<String, TopicSettings> topics = defaultTopics();

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
        return topics.get(topic);
    }

    public double displaySeconds(String topic) {
        TopicSettings settings = topic(topic);
        return settings == null ? DEFAULT_DISPLAY_SECONDS : settings.displaySeconds;
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
        topics.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null);
        topics.values().forEach(TopicSettings::sanitize);
        defaultTopics().forEach(topics::putIfAbsent);
    }

    private static Map<String, TopicSettings> defaultTopics() {
        LinkedHashMap<String, TopicSettings> defaults = new LinkedHashMap<>();
        defaults.put("dungeon", new TopicSettings(DEFAULT_DISPLAY_SECONDS, DUNGEON_TEMPLATE));
        defaults.put("mastery", new TopicSettings(DEFAULT_DISPLAY_SECONDS, MASTERY_TEMPLATE));
        defaults.put("update", new TopicSettings(60.0D, UPDATE_TEMPLATE));
        return defaults;
    }

    public static TopicSettings defaultTopic(String topic) {
        TopicSettings settings = defaultTopics().get(topic);
        return settings == null ? new TopicSettings(DEFAULT_DISPLAY_SECONDS, "") : settings;
    }

    public static final class TopicSettings {
        public double displaySeconds = DEFAULT_DISPLAY_SECONDS;
        public String template = "";

        public TopicSettings() {
        }

        public TopicSettings(double displaySeconds, String template) {
            this.displaySeconds = displaySeconds;
            this.template = template;
            sanitize();
        }

        private void sanitize() {
            displaySeconds = Math.max(0.5D, Math.min(600.0D, displaySeconds));
            if (template == null) {
                template = "";
            } else if (template.length() > 8192) {
                template = template.substring(0, 8192);
            }
        }
    }
}
