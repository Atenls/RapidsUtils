package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.display.HudLine;
import com.atenls.rapidsutils.display.JsonDisplayFormatter;
import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import com.atenls.rapidsutils.text.MinecraftColorParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class RapidsHudRenderer {
    private static final int PANEL_PADDING = 8;
    private static final int MIN_PANEL_WIDTH = 156;
    private static final int LINE_HEIGHT = 10;
    private static final int TOPIC_GAP = 5;
    private static final int INDENT_WIDTH = 10;
    private static final int BACKGROUND_RGB = 0x050607;
    private static final int ACCENT = 0x8BD5CA;
    private static final int TITLE = 0xD5DAE0;
    private static final int META = 0x737D89;

    private final TopicSnapshotStore store;
    private final RapidsConfig config;
    private final JsonDisplayFormatter formatter = new JsonDisplayFormatter();

    public RapidsHudRenderer(TopicSnapshotStore store, RapidsConfig config) {
        this.store = store;
        this.config = config;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        List<DataEnvelope> topics = store.snapshot().newestFirst();
        if (!config.enabled || topics.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int availableWidth = context.getScaledWindowWidth() - config.margin * 2;
        int availableHeight = context.getScaledWindowHeight() - config.margin * 2;
        if (availableWidth < 40 || availableHeight < 30) {
            return;
        }

        int panelMaxWidth = Math.min(config.maxWidth, availableWidth);
        int contentMaxWidth = Math.max(20, panelMaxWidth - PANEL_PADDING * 2);
        int maxRows = Math.max(1, (availableHeight - PANEL_PADDING * 2) / LINE_HEIGHT);
        List<RenderRow> rows = buildRows(textRenderer, topics, contentMaxWidth, maxRows);
        if (rows.isEmpty()) {
            return;
        }

        int contentWidth = rows.stream()
                .mapToInt(row -> row.indent() + textRenderer.getWidth(row.text()))
                .max()
                .orElse(0);
        int panelWidth = Math.min(panelMaxWidth, Math.max(Math.min(MIN_PANEL_WIDTH, panelMaxWidth), contentWidth + PANEL_PADDING * 2));
        int panelHeight = PANEL_PADDING * 2 + rows.stream().mapToInt(RenderRow::height).sum();
        int x = Math.min(config.margin, Math.max(0, context.getScaledWindowWidth() - panelWidth));
        int y = Math.min(config.margin, Math.max(0, context.getScaledWindowHeight() - panelHeight));

        fillRounded(context, x, y, panelWidth, panelHeight, 8, alphaColor(config.backgroundOpacity, BACKGROUND_RGB));
        int rowY = y + PANEL_PADDING;
        for (RenderRow row : rows) {
            rowY += row.gapBefore();
            context.drawText(textRenderer, row.text(), x + PANEL_PADDING + row.indent(), rowY, 0xFFFFFFFF, true);
            rowY += LINE_HEIGHT;
        }
    }

    private List<RenderRow> buildRows(TextRenderer renderer, List<DataEnvelope> topics, int maxWidth, int maxRows) {
        ArrayList<RenderRow> rows = new ArrayList<>();
        rows.add(new RenderRow(0, 0, styledText(List.of(
                new MinecraftColorParser.Segment("◆ ", ACCENT),
                new MinecraftColorParser.Segment("RAPIDS DATA", TITLE)
        ), true).asOrderedText()));

        boolean truncated = false;
        for (int topicIndex = 0; topicIndex < topics.size(); topicIndex++) {
            DataEnvelope topic = topics.get(topicIndex);
            if (rows.size() >= maxRows) {
                truncated = true;
                break;
            }

            int gap = topicIndex == 0 ? 2 : TOPIC_GAP;
            Text topicText = styledText(List.of(
                    new MinecraftColorParser.Segment(topic.topic(), TITLE),
                    new MinecraftColorParser.Segment("  #" + topic.sequence(), META)
            ), true);
            rows.add(new RenderRow(0, gap, topicText.asOrderedText()));

            for (HudLine logical : formatter.format(topic.data())) {
                int indent = Math.min(INDENT_WIDTH * (logical.depth() + 1), Math.max(0, maxWidth / 2));
                Text text = styledText(logical.spans(), false);
                List<OrderedText> wrapped = renderer.wrapLines(text, Math.max(20, maxWidth - indent));
                int wrapCount = Math.min(3, wrapped.size());
                for (int index = 0; index < wrapCount; index++) {
                    if (rows.size() >= maxRows) {
                        truncated = true;
                        break;
                    }
                    rows.add(new RenderRow(indent, 0, wrapped.get(index)));
                }
                if (rows.size() >= maxRows) {
                    break;
                }
            }
            if (truncated || rows.size() >= maxRows) {
                truncated = true;
                break;
            }
        }

        if (truncated && maxRows > 1) {
            if (rows.size() >= maxRows) {
                rows.removeLast();
            }
            rows.add(new RenderRow(INDENT_WIDTH, 0, Text.literal("More data…").styled(style -> style.withColor(META)).asOrderedText()));
        }
        return List.copyOf(rows);
    }

    private static Text styledText(List<MinecraftColorParser.Segment> spans, boolean bold) {
        MutableText text = Text.empty();
        for (MinecraftColorParser.Segment span : spans) {
            text.append(Text.literal(span.text()).styled(style -> style.withColor(span.color()).withBold(bold)));
        }
        return text;
    }

    private static void fillRounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int inset = Math.min(radius, Math.min(width / 2, height / 2));
        context.fill(x + inset, y, x + width - inset, y + height, color);
        context.fill(x + 3, y + 1, x + width - 3, y + height - 1, color);
        context.fill(x + 1, y + 3, x + width - 1, y + height - 3, color);
        context.fill(x, y + inset, x + width, y + height - inset, color);
    }

    private static int alphaColor(float opacity, int rgb) {
        int alpha = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private record RenderRow(int indent, int gapBefore, OrderedText text) {
        private int height() {
            return LINE_HEIGHT + gapBefore;
        }
    }
}
