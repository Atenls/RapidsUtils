package com.atenls.rapidsutils.client.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RapidsConfigScreen extends Screen {
    private static final int CONTROL_WIDTH = 280;
    private static final int CONTROL_HEIGHT = 20;

    private final Screen parent;
    private final RapidsConfig config;
    private boolean enabled;
    private float backgroundOpacity;
    private int margin;
    private int maxWidth;
    private RapidsConfig.VitalsBarStyle vitalsBarStyle;
    private int topicPage;

    public RapidsConfigScreen(Screen parent) {
        super(Text.literal("RapidsUtils 设置"));
        this.parent = parent;
        this.config = RapidsConfig.get();
        this.enabled = config.enabled;
        this.backgroundOpacity = config.backgroundOpacity;
        this.margin = config.margin;
        this.maxWidth = config.maxWidth;
        this.vitalsBarStyle = config.vitalsBarStyle;
    }

    @Override
    protected void init() {
        int controlX = (width - CONTROL_WIDTH) / 2;
        int y = 34;

        addDrawableChild(ButtonWidget.builder(enabledMessage(), button -> {
                    enabled = !enabled;
                    button.setMessage(enabledMessage());
                })
                .dimensions(controlX, y, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build());
        addDrawableChild(new ConfigSlider(controlX, y + 24, opacityMessage(), opacityToSlider(backgroundOpacity)) {
            @Override
            protected void updateValue(double value) {
                backgroundOpacity = sliderToOpacity(value);
            }

            @Override
            protected Text message() {
                return opacityMessage();
            }
        });
        addDrawableChild(new ConfigSlider(controlX, y + 48, marginMessage(), margin / 64.0D) {
            @Override
            protected void updateValue(double value) {
                margin = (int) Math.round(value * 64.0D);
            }

            @Override
            protected Text message() {
                return marginMessage();
            }
        });
        addDrawableChild(new ConfigSlider(controlX, y + 72, widthMessage(), widthToSlider(maxWidth)) {
            @Override
            protected void updateValue(double value) {
                maxWidth = sliderToWidth(value);
            }

            @Override
            protected Text message() {
                return widthMessage();
            }
        });
        addDrawableChild(ButtonWidget.builder(vitalsBarStyleMessage(), button -> {
                    vitalsBarStyle = vitalsBarStyle.next();
                    button.setMessage(vitalsBarStyleMessage());
                })
                .dimensions(controlX, y + 96, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build());

        List<String> topicNames = new ArrayList<>(config.topics.keySet());
        int topicsY = 172;
        int navigationY = height - 52;
        int pageSize = Math.max(1, (navigationY - topicsY) / 22);
        int pageCount = Math.max(1, (topicNames.size() + pageSize - 1) / pageSize);
        topicPage = Math.max(0, Math.min(topicPage, pageCount - 1));
        int start = topicPage * pageSize;
        int end = Math.min(topicNames.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            String topic = topicNames.get(index);
            RapidsConfig.TopicSettings settings = config.topic(topic);
            Text message = Text.literal(String.format(
                    Locale.ROOT,
                    "%s  ·  索引 %d  ·  回退 %.1f 秒",
                    topic,
                    settings.index,
                    settings.displaySeconds
            ));
            int rowY = topicsY + (index - start) * 22;
            addDrawableChild(ButtonWidget.builder(message, button -> openTopic(topic))
                    .dimensions(controlX, rowY, CONTROL_WIDTH, CONTROL_HEIGHT)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
                    topicPage--;
                    clearAndInit();
                })
                .dimensions(controlX, navigationY, 42, CONTROL_HEIGHT)
                .build()).active = topicPage > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal("新增主题"), button -> openTopic(null))
                .dimensions(controlX + 48, navigationY, CONTROL_WIDTH - 96, CONTROL_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
                    topicPage++;
                    clearAndInit();
                })
                .dimensions(controlX + CONTROL_WIDTH - 42, navigationY, 42, CONTROL_HEIGHT)
                .build()).active = topicPage + 1 < pageCount;

        int bottomY = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), button -> saveAndClose())
                .dimensions(controlX, bottomY, 136, CONTROL_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), button -> close())
                .dimensions(controlX + 144, bottomY, 136, CONTROL_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("主题"), width / 2, 156, 0xFFB8C0CC);
    }

    private void openTopic(String topic) {
        if (client != null) {
            client.setScreen(new TopicConfigScreen(this, config, topic));
        }
    }

    private void saveAndClose() {
        config.enabled = enabled;
        config.backgroundOpacity = backgroundOpacity;
        config.margin = margin;
        config.maxWidth = maxWidth;
        config.vitalsBarStyle = vitalsBarStyle;
        config.save();
        close();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private Text enabledMessage() {
        return Text.literal("HUD 显示：" + (enabled ? "开启" : "关闭"));
    }

    private Text opacityMessage() {
        return Text.literal("背景不透明度：" + Math.round(backgroundOpacity * 100.0F) + "%");
    }

    private Text marginMessage() {
        return Text.literal("屏幕边距：" + margin + "px");
    }

    private Text widthMessage() {
        return Text.literal("最大宽度：" + maxWidth + "px");
    }

    private Text vitalsBarStyleMessage() {
        return Text.literal("血条样式：" + vitalsBarStyle.displayName());
    }

    private static float sliderToOpacity(double value) {
        return (float) (0.15D + value * 0.80D);
    }

    private static double opacityToSlider(float opacity) {
        return (Math.max(0.15F, Math.min(0.95F, opacity)) - 0.15D) / 0.80D;
    }

    private static int sliderToWidth(double value) {
        return 140 + (int) Math.round(value * 460.0D);
    }

    private static double widthToSlider(int width) {
        return (Math.max(140, Math.min(600, width)) - 140) / 460.0D;
    }

    private abstract static class ConfigSlider extends SliderWidget {
        ConfigSlider(int x, int y, Text message, double value) {
            super(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, message, value);
        }

        @Override
        protected void updateMessage() {
            setMessage(message());
        }

        @Override
        protected void applyValue() {
            updateValue(value);
            updateMessage();
        }

        protected abstract void updateValue(double value);

        protected abstract Text message();
    }
}
