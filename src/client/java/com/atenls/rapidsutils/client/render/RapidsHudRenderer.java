package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.display.HudLine;
import com.atenls.rapidsutils.display.JsonDisplayFormatter;
import com.atenls.rapidsutils.display.TopicTemplateFormatter;
import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.state.TopicSnapshot;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import com.atenls.rapidsutils.text.MinecraftColorParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RapidsHudRenderer {
    private static final int PANEL_PADDING = 8;
    private static final int PANEL_GAP = 6;
    private static final int MIN_PANEL_WIDTH = 120;
    private static final int LINE_HEIGHT = 10;
    private static final int INDENT_WIDTH = 10;
    private static final int BACKGROUND_RGB = 0x050607;
    private static final int ACCENT = 0x8BD5CA;
    private static final int TITLE = 0xD5DAE0;
    private static final int META = 0x737D89;

    private final TopicSnapshotStore store;
    private final RapidsConfig config;
    private final JsonDisplayFormatter jsonFormatter = new JsonDisplayFormatter();
    private final TopicTemplateFormatter templateFormatter = new TopicTemplateFormatter();

    public RapidsHudRenderer(TopicSnapshotStore store, RapidsConfig config) {
        this.store = store;
        this.config = config;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!config.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int availableWidth = screenWidth - config.margin * 2;
        if (availableWidth < 40) {
            return;
        }

        int panelMaxWidth = Math.min(config.maxWidth, availableWidth);
        int stackedY = config.margin;
        long currentTick = store.currentTick();
        for (TopicSnapshot snapshot : store.snapshot().orderedForHud(config::index)) {
            DataEnvelope envelope = snapshot.envelope();
            BigDecimal fallbackDurationTicks = BigDecimal.valueOf(config.displaySeconds(envelope.topic()))
                    .multiply(BigDecimal.valueOf(20L));
            if (!snapshot.isVisibleAt(currentTick, fallbackDurationTicks)) {
                continue;
            }

            boolean serverPositionedY = envelope.screenY().isPresent();
            int availableHeight = serverPositionedY
                    ? screenHeight
                    : screenHeight - config.margin - stackedY;
            if (availableHeight < PANEL_PADDING * 2 + LINE_HEIGHT) {
                continue;
            }

            List<HudLine> logicalLines = linesFor(envelope);
            RenderPanel panel = layoutPanel(textRenderer, logicalLines, panelMaxWidth, availableHeight);
            if (panel.rows().isEmpty()) {
                continue;
            }

            int defaultX = Math.min(config.margin, Math.max(0, screenWidth - panel.width()));
            int x = centeredCoordinate(envelope.resolvedScreenX(screenWidth), panel.width(), defaultX);
            int y = centeredCoordinate(envelope.resolvedScreenY(screenHeight), panel.height(), stackedY);
            float opacity = envelope.panelOpacity()
                    .map(BigDecimal::floatValue)
                    .orElse(config.backgroundOpacity);
            fillRounded(
                    context,
                    x,
                    y,
                    panel.width(),
                    panel.height(),
                    8,
                    alphaColor(opacity, BACKGROUND_RGB)
            );
            int rowY = y + PANEL_PADDING;
            for (RenderRow row : panel.rows()) {
                context.drawText(textRenderer, row.text(), x + PANEL_PADDING + row.indent(), rowY, 0xFFFFFFFF, true);
                rowY += LINE_HEIGHT;
            }
            if (!serverPositionedY) {
                stackedY += panel.height() + PANEL_GAP;
            }
        }
    }

    private List<HudLine> linesFor(DataEnvelope envelope) {
        String hiddenTemplate = RapidsConfig.hiddenTemplate(envelope.topic());
        if (hiddenTemplate != null) {
            return templateFormatter.format(hiddenTemplate, envelope.data());
        }

        RapidsConfig.TopicSettings settings = config.topic(envelope.topic());
        if (settings != null && !settings.template.isBlank()) {
            return templateFormatter.format(settings.template, envelope.data());
        }

        ArrayList<HudLine> lines = new ArrayList<>();
        lines.add(new HudLine(0, List.of(
                new MinecraftColorParser.Segment("◆ ", ACCENT),
                new MinecraftColorParser.Segment(envelope.topic(), TITLE)
        )));
        for (HudLine line : jsonFormatter.format(envelope.data())) {
            lines.add(new HudLine(line.depth() + 1, line.spans()));
        }
        return List.copyOf(lines);
    }

    private RenderPanel layoutPanel(TextRenderer renderer, List<HudLine> logicalLines, int panelMaxWidth, int availableHeight) {
        int contentMaxWidth = Math.max(20, panelMaxWidth - PANEL_PADDING * 2);
        int maxRows = Math.max(1, (availableHeight - PANEL_PADDING * 2) / LINE_HEIGHT);
        ArrayList<RenderRow> rows = new ArrayList<>();
        boolean truncated = false;

        for (HudLine logical : logicalLines) {
            int indent = Math.min(INDENT_WIDTH * logical.depth(), Math.max(0, contentMaxWidth / 2));
            Text text = styledText(logical.spans());
            List<OrderedText> wrapped = renderer.wrapLines(text, Math.max(20, contentMaxWidth - indent));
            if (wrapped.isEmpty()) {
                wrapped = List.of(Text.empty().asOrderedText());
            }
            int wrapCount = Math.min(3, wrapped.size());
            for (int index = 0; index < wrapCount; index++) {
                if (rows.size() >= maxRows) {
                    truncated = true;
                    break;
                }
                rows.add(new RenderRow(indent, wrapped.get(index)));
            }
            if (truncated) {
                break;
            }
        }

        if (truncated && maxRows > 1) {
            if (rows.size() >= maxRows) {
                rows.removeLast();
            }
            rows.add(new RenderRow(INDENT_WIDTH, Text.literal("More data…")
                    .styled(style -> style.withColor(META))
                    .asOrderedText()));
        }

        int contentWidth = rows.stream()
                .mapToInt(row -> row.indent() + renderer.getWidth(row.text()))
                .max()
                .orElse(0);
        int panelWidth = Math.min(
                panelMaxWidth,
                Math.max(Math.min(MIN_PANEL_WIDTH, panelMaxWidth), contentWidth + PANEL_PADDING * 2)
        );
        int panelHeight = PANEL_PADDING * 2 + rows.size() * LINE_HEIGHT;
        return new RenderPanel(panelWidth, panelHeight, List.copyOf(rows));
    }

    private static Text styledText(List<MinecraftColorParser.Segment> spans) {
        MutableText text = Text.empty();
        for (MinecraftColorParser.Segment span : spans) {
            text.append(Text.literal(span.text()).styled(style -> style.withColor(span.color())));
        }
        return text;
    }

    private static void fillRounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int actualRadius = Math.min(radius, Math.min(width / 2, height / 2));
        for (int row = 0; row < height; row++) {
            int distanceFromEdge = Math.min(row, height - row - 1);
            int inset = 0;
            if (distanceFromEdge < actualRadius) {
                double vertical = actualRadius - distanceFromEdge - 0.5D;
                inset = (int) Math.ceil(actualRadius - Math.sqrt(actualRadius * actualRadius - vertical * vertical));
            }
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    private static int alphaColor(float opacity, int rgb) {
        int alpha = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int centeredCoordinate(Optional<BigDecimal> center, int size, int fallback) {
        if (center.isEmpty()) {
            return fallback;
        }
        double coordinate = center.orElseThrow().doubleValue() - size / 2.0D;
        if (!Double.isFinite(coordinate)) {
            return fallback;
        }
        return (int) Math.round(Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, coordinate)));
    }

    private record RenderRow(int indent, OrderedText text) {
    }

    private record RenderPanel(int width, int height, List<RenderRow> rows) {
    }
}
