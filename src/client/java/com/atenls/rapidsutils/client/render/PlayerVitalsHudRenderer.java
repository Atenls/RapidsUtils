package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.protocol.PlayerVitals;
import com.atenls.rapidsutils.state.PlayerVitalsState;
import com.atenls.rapidsutils.util.RoundingUtil;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;

public final class PlayerVitalsHudRenderer {
    public static final Identifier ID = Identifier.of(RapidsUtilsClient.MOD_ID, "player_vitals");
    public static final int STATUS_BAR_HEIGHT = 11;

    private static final int GROUP_WIDTH = 182;
    private static final float BAR_WIDTH_RATIO = 0.4F;
    private static final int BAR_HEIGHT = 11;
    private static final int CORNER_RADIUS = 3;
    private static final float LOW_HEALTH_THRESHOLD = 0.25F;
    private static final int HEALTH_CRITICAL = 0x78131D;
    private static final int HEALTH_HEALTHY = 0xF07D7D;
    private static final int MANA_BLUE = 0x439ED1;
    private static final int TEXT_COLOR = 0xFFF5F6F7;
    private static final int[] LOW_HEALTH_SHAKE_X = {0, -1, 1, -1, 1, 0, 0, 0, 0};
    private static final int[] LOW_HEALTH_SHAKE_Y = {0, 0, 0, 1, 0, 0, 0, 0, 0};

    private final PlayerVitalsState state;

    public PlayerVitalsHudRenderer(PlayerVitalsState state) {
        this.state = state;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        state.snapshot().ifPresent(vitals -> renderBars(context, tickCounter, vitals));
    }

    private static void renderBars(DrawContext context, RenderTickCounter tickCounter, PlayerVitals vitals) {
        int groupWidth = Math.min(GROUP_WIDTH, context.getScaledWindowWidth() - 12);
        int barWidth = Math.round(groupWidth * BAR_WIDTH_RATIO);
        if (barWidth < 48) {
            return;
        }

        int groupX = (context.getScaledWindowWidth() - groupWidth) / 2;
        int y = context.getScaledWindowHeight()
                - HudStatusBarHeightRegistry.getHeight(ID)
                - BAR_HEIGHT;
        float healthRatio = vitals.healthRatio();
        int healthColor = mix(HEALTH_CRITICAL, HEALTH_HEALTHY, healthRatio);
        LowHealthMotion motion = lowHealthMotion(tickCounter, healthRatio);

        drawBar(
                context,
                groupX + motion.x(),
                y + motion.y(),
                barWidth,
                vitals.health(),
                healthRatio,
                healthColor
        );
        if (motion.dropProgress() >= 0.0F) {
            drawBloodDrop(
                    context,
                    groupX + motion.x(),
                    y + motion.y(),
                    barWidth,
                    healthRatio,
                    healthColor,
                    motion.dropProgress()
            );
        }
        drawBar(
                context,
                groupX + groupWidth - barWidth,
                y,
                barWidth,
                vitals.mana(),
                vitals.manaRatio(),
                MANA_BLUE
        );
    }

    private static void drawBar(
            DrawContext context,
            int x,
            int y,
            int width,
            BigDecimal value,
            float ratio,
            int fillColor
    ) {
        int borderColor = mix(fillColor, 0x090A0C, 0.58F);
        fillRounded(context, x, y, width, BAR_HEIGHT, CORNER_RADIUS, 0xFF000000 | borderColor);

        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;
        int innerHeight = BAR_HEIGHT - 2;
        fillRounded(
                context,
                innerX,
                innerY,
                innerWidth,
                innerHeight,
                CORNER_RADIUS - 1,
                alphaColor(0.2F, fillColor)
        );

        int filledWidth = Math.round(innerWidth * ratio);
        if (ratio > 0.0F) {
            filledWidth = Math.max(1, filledWidth);
        }
        if (filledWidth > 0) {
            fillRoundedClipped(
                    context,
                    innerX,
                    innerY,
                    innerWidth,
                    innerHeight,
                    CORNER_RADIUS - 1,
                    0xFF000000 | fillColor,
                    innerX + filledWidth
            );
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        String text = RoundingUtil.longFormat(value);
        int textX = x + (width - renderer.getWidth(text)) / 2;
        context.drawText(renderer, text, textX, y + 2, TEXT_COLOR, false);
    }

    private static LowHealthMotion lowHealthMotion(RenderTickCounter tickCounter, float healthRatio) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (healthRatio > LOW_HEALTH_THRESHOLD || client.world == null) {
            return LowHealthMotion.NONE;
        }

        double animationTick = client.world.getTime() + tickCounter.getTickProgress(false);
        long wholeTick = (long) Math.floor(animationTick);
        long cycle = Math.floorDiv(wholeTick, 60L);
        int cycleTick = (int) Math.floorMod(wholeTick, 60L);
        int eventStart = 8 + (int) Math.floorMod(cycle * 1_103_515_245L + 12_345L, 31L);
        int eventTick = cycleTick - eventStart;
        if (eventTick < 0 || eventTick >= 9) {
            return LowHealthMotion.NONE;
        }

        int intensity = healthRatio <= 0.1F ? 2 : 1;
        float dropProgress = eventTick < 2 ? -1.0F : (eventTick - 2) / 6.0F;
        return new LowHealthMotion(
                LOW_HEALTH_SHAKE_X[eventTick] * intensity,
                LOW_HEALTH_SHAKE_Y[eventTick],
                dropProgress
        );
    }

    private static void drawBloodDrop(
            DrawContext context,
            int x,
            int y,
            int width,
            float ratio,
            int color,
            float progress
    ) {
        int innerWidth = width - 2;
        int dropX = x + 1 + Math.round(innerWidth * ratio);
        int dropY = y + BAR_HEIGHT + Math.round(progress * 4.0F);
        int alpha = Math.round((1.0F - progress * 0.35F) * 255.0F);
        context.fill(dropX, dropY, dropX + 1, dropY + 2, (alpha << 24) | color);
    }

    private static void fillRounded(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        fillRoundedClipped(context, x, y, width, height, radius, color, x + width);
    }

    private static void fillRoundedClipped(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color,
            int clipRight
    ) {
        int actualRadius = Math.min(radius, Math.min(width / 2, height / 2));
        for (int row = 0; row < height; row++) {
            int distanceFromEdge = Math.min(row, height - row - 1);
            int inset = 0;
            if (distanceFromEdge < actualRadius) {
                double vertical = actualRadius - distanceFromEdge - 0.5D;
                inset = (int) Math.ceil(actualRadius
                        - Math.sqrt(actualRadius * actualRadius - vertical * vertical));
            }
            int rowStart = x + inset;
            int rowEnd = Math.min(x + width - inset, clipRight);
            if (rowEnd > rowStart) {
                context.fill(rowStart, y + row, rowEnd, y + row + 1, color);
            }
        }
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

    private record LowHealthMotion(int x, int y, float dropProgress) {
        private static final LowHealthMotion NONE = new LowHealthMotion(0, 0, -1.0F);
    }
}
