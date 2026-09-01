package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.protocol.PlayerVitals;
import com.atenls.rapidsutils.state.PlayerVitalsState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PlayerVitalsHudRenderer {
    public static final Identifier ID = Identifier.of(RapidsUtilsClient.MOD_ID, "player_vitals");
    public static final int STATUS_BAR_HEIGHT = 25;

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 11;
    private static final int BAR_GAP = 3;
    private static final int HEALTH_GREEN = 0x66C38A;
    private static final int HEALTH_GREEN_BORDER = 0x58675F;
    private static final int HEALTH_BLUE = 0x66A9CC;
    private static final int HEALTH_BLUE_BORDER = 0x56646C;
    private static final int HEALTH_RED = 0xD06F72;
    private static final int HEALTH_RED_BORDER = 0x6B595B;
    private static final int MANA_BLUE = 0x7188D8;
    private static final int MANA_BLUE_BORDER = 0x5A6176;
    private static final int TEXT_COLOR = 0xFFF4F6F7;

    private final PlayerVitalsState state;

    public PlayerVitalsHudRenderer(PlayerVitalsState state) {
        this.state = state;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        state.snapshot().ifPresent(vitals -> renderBars(context, vitals));
    }

    private static void renderBars(DrawContext context, PlayerVitals vitals) {
        int width = Math.min(BAR_WIDTH, context.getScaledWindowWidth() - 12);
        if (width < 60) {
            return;
        }

        int x = (context.getScaledWindowWidth() - width) / 2;
        int bottom = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(ID);
        int manaY = bottom - BAR_HEIGHT;
        int healthY = manaY - BAR_GAP - BAR_HEIGHT;

        BarColors healthColors = switch (vitals.healthBand()) {
            case GREEN -> new BarColors(HEALTH_GREEN, HEALTH_GREEN_BORDER);
            case BLUE -> new BarColors(HEALTH_BLUE, HEALTH_BLUE_BORDER);
            case RED -> new BarColors(HEALTH_RED, HEALTH_RED_BORDER);
        };
        drawBar(
                context, x, healthY, width, "HP",
                vitals.health(), vitals.healthMax(), vitals.healthRegen(),
                vitals.healthRatio(), healthColors
        );
        drawBar(
                context, x, manaY, width, "MP",
                vitals.mana(), vitals.manaMax(), vitals.manaRegen(),
                vitals.manaRatio(), new BarColors(MANA_BLUE, MANA_BLUE_BORDER)
        );
    }

    private static void drawBar(
            DrawContext context,
            int x,
            int y,
            int width,
            String label,
            BigDecimal value,
            BigDecimal maximum,
            BigDecimal regen,
            float ratio,
            BarColors colors
    ) {
        fillChamfered(context, x + 1, y + 1, width, BAR_HEIGHT, 0x65000000);
        fillChamfered(context, x, y, width, BAR_HEIGHT, 0xFF000000 | colors.border());

        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;
        int innerHeight = BAR_HEIGHT - 2;
        context.fill(
                innerX, innerY, innerX + innerWidth, innerY + innerHeight,
                alphaColor(0.2F, colors.fill())
        );

        int filledWidth = Math.round(innerWidth * ratio);
        if (ratio > 0.0F) {
            filledWidth = Math.max(1, filledWidth);
        }
        if (filledWidth > 0) {
            context.fill(innerX, innerY, innerX + filledWidth, innerY + innerHeight,
                    alphaColor(0.92F, colors.fill()));
            context.fill(innerX, innerY, innerX + filledWidth, innerY + 1,
                    alphaColor(0.82F, mix(colors.fill(), 0xFFFFFF, 0.28F)));
            context.fill(innerX, innerY + innerHeight - 1, innerX + filledWidth, innerY + innerHeight,
                    alphaColor(0.75F, mix(colors.fill(), 0x000000, 0.24F)));
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        String valueText = label + " " + format(value) + "/" + format(maximum);
        String regenText = signed(regen);
        int textY = y + 2;
        context.drawText(renderer, valueText, x + 5, textY, TEXT_COLOR, true);
        context.drawText(renderer, regenText, x + width - 5 - renderer.getWidth(regenText), textY, TEXT_COLOR, true);
    }

    private static void fillChamfered(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x + 1, y, x + width - 1, y + 1, color);
        context.fill(x, y + 1, x + width, y + height - 1, color);
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, color);
    }

    private static String format(BigDecimal value) {
        BigDecimal rounded = value.stripTrailingZeros();
        if (rounded.scale() > 1) {
            rounded = rounded.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        }
        return rounded.toPlainString();
    }

    private static String signed(BigDecimal value) {
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + format(value);
    }

    private static int alphaColor(float opacity, int rgb) {
        int alpha = Math.round(Math.clamp(opacity, 0.0F, 1.0F) * 255.0F);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int first, int second, float weight) {
        float clamped = Math.clamp(weight, 0.0F, 1.0F);
        int red = Math.round(((first >> 16) & 0xFF) * (1.0F - clamped) + ((second >> 16) & 0xFF) * clamped);
        int green = Math.round(((first >> 8) & 0xFF) * (1.0F - clamped) + ((second >> 8) & 0xFF) * clamped);
        int blue = Math.round((first & 0xFF) * (1.0F - clamped) + (second & 0xFF) * clamped);
        return (red << 16) | (green << 8) | blue;
    }

    private record BarColors(int fill, int border) {
    }
}
