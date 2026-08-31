package com.atenls.rapidsutils.client.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class TopicConfigScreen extends Screen {
    private static final int CONTROL_HEIGHT = 20;
    private static final double MIN_SECONDS = 0.5D;
    private static final double MAX_SECONDS = 60.0D;

    private final Screen parent;
    private final RapidsConfig config;
    private final String originalTopic;
    private String initialTemplate;
    private double displaySeconds;
    private int topicIndex;
    private TextFieldWidget topicField;
    private TextFieldWidget indexField;
    private EditBoxWidget templateEditor;
    private String error = "";

    public TopicConfigScreen(Screen parent, RapidsConfig config, String topic) {
        super(Text.literal(topic == null ? "新增主题" : "编辑主题"));
        this.parent = parent;
        this.config = config;
        this.originalTopic = topic;
        RapidsConfig.TopicSettings settings = topic == null ? null : config.topic(topic);
        this.displaySeconds = settings == null ? RapidsConfig.DEFAULT_DISPLAY_SECONDS : settings.displaySeconds;
        this.topicIndex = settings == null ? RapidsConfig.DEFAULT_TOPIC_INDEX : settings.index;
        this.initialTemplate = settings == null ? "" : settings.template;
    }

    @Override
    protected void init() {
        int controlWidth = Math.min(380, width - 40);
        int controlX = (width - controlWidth) / 2;

        topicField = new TextFieldWidget(textRenderer, controlX, 47, controlWidth, CONTROL_HEIGHT, Text.literal("主题 ID"));
        topicField.setMaxLength(64);
        topicField.setText(originalTopic == null ? "" : originalTopic);
        topicField.setPlaceholder(Text.literal("主题名称"));
        addDrawableChild(topicField);

        addDrawableChild(new DurationSlider(controlX, 76, controlWidth, secondsToSlider(displaySeconds)));

        indexField = new TextFieldWidget(textRenderer, controlX, 108, controlWidth, CONTROL_HEIGHT, Text.literal("排序索引"));
        indexField.setMaxLength(11);
        indexField.setText(Integer.toString(topicIndex));
        indexField.setPlaceholder(Text.literal("10"));
        addDrawableChild(indexField);

        int editorY = 151;
        int bottomY = height - 27;
        int editorHeight = Math.max(50, bottomY - editorY - 10);
        templateEditor = EditBoxWidget.builder()
                .x(controlX)
                .y(editorY)
                .placeholder(Text.literal("留空时以通用 JSON 格式显示"))
                .hasBackground(true)
                .build(textRenderer, controlWidth, editorHeight, Text.empty());
        templateEditor.setMaxLength(8192);
        templateEditor.setMaxLines(64);
        templateEditor.setText(initialTemplate);
        addDrawableChild(templateEditor);

        int buttonWidth = (controlWidth - 16) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), button -> saveAndClose())
                .dimensions(controlX, bottomY, buttonWidth, CONTROL_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("恢复默认"), button -> resetTemplate())
                .dimensions(controlX + buttonWidth + 8, bottomY, buttonWidth, CONTROL_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), button -> close())
                .dimensions(controlX + (buttonWidth + 8) * 2, bottomY, buttonWidth, CONTROL_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        int labelX = (width - Math.min(380, width - 40)) / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("主题 ID"), labelX, 35, 0xFFB8C0CC);
        context.drawTextWithShadow(textRenderer, Text.literal("排序索引（服务端 index 为空时）"), labelX, 96, 0xFFB8C0CC);
        Text hint = error.isEmpty()
                ? Text.literal("模板 · {rhombus}、{dataKey}、嵌套 {key.child}、RGB &#rrggbb")
                : Text.literal(error);
        context.drawTextWithShadow(textRenderer, hint, labelX, 136, error.isEmpty() ? 0xFF8F99A6 : 0xFFFF5555);
    }

    private void saveAndClose() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            error = "主题 ID 不能为空";
            return;
        }
        if (topic.length() > 64) {
            error = "主题 ID 过长";
            return;
        }
        if (RapidsConfig.isHiddenTopic(topic)) {
            error = "该主题为内置隐藏主题，无法编辑";
            return;
        }
        if (!topic.equals(originalTopic) && config.topics.containsKey(topic)) {
            error = "该主题已存在";
            return;
        }
        try {
            topicIndex = Integer.parseInt(indexField.getText().trim());
        } catch (NumberFormatException e) {
            error = "排序索引必须是整数";
            return;
        }

        if (originalTopic != null && !originalTopic.equals(topic)) {
            config.removeTopic(originalTopic);
        }
        config.putTopic(topic, new RapidsConfig.TopicSettings(displaySeconds, topicIndex, templateEditor.getText()));
        config.save();
        close();
    }

    private void resetTemplate() {
        String topic = topicField.getText().trim();
        RapidsConfig.TopicSettings defaults = RapidsConfig.defaultTopic(topic);
        displaySeconds = defaults.displaySeconds;
        topicIndex = defaults.index;
        initialTemplate = defaults.template;
        clearAndInit();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private Text durationMessage() {
        return Text.literal(String.format(Locale.ROOT, "duration 为空时显示：%.1f 秒", displaySeconds));
    }

    private static double sliderToSeconds(double value) {
        return MIN_SECONDS + value * (MAX_SECONDS - MIN_SECONDS);
    }

    private static double secondsToSlider(double seconds) {
        double clamped = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
        return (clamped - MIN_SECONDS) / (MAX_SECONDS - MIN_SECONDS);
    }

    private final class DurationSlider extends SliderWidget {
        private DurationSlider(int x, int y, int width, double value) {
            super(x, y, width, CONTROL_HEIGHT, durationMessage(), value);
        }

        @Override
        protected void updateMessage() {
            setMessage(durationMessage());
        }

        @Override
        protected void applyValue() {
            displaySeconds = sliderToSeconds(value);
            updateMessage();
        }
    }
}
