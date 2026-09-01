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
    public static final int STATUS_BAR_HEIGHT = 13;

    private static final int GROUP_WIDTH = 182;
    private static final float BAR_WIDTH_RATIO = 0.4F;
    private static final int BAR_HEIGHT = 13;
    private static final int TRACK_INSET = 2;
    private static final float LOW_HEALTH_THRESHOLD = 0.25F;
    private static final int HEALTH_CRITICAL = 0x77131F;
    private static final int HEALTH_HEALTHY = 0xD94352;
    private static final int MANA_BLUE = 0x2688C7;
    private static final int TEXT_COLOR = 0xFFF8F4EC;
    private static final int FRAME_DARK = 0xFF090C10;
    private static final int FRAME_MID = 0xFF303943;
    private static final int FRAME_LIGHT = 0xFF66737E;
    private static final int TRACK_DARK = 0xFF080B0F;
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
        fillChamfered(context, x + 1, y + 1, width, BAR_HEIGHT, 0x70000000);
        fillChamfered(context, x, y, width, BAR_HEIGHT, FRAME_DARK);
        fillChamfered(context, x + 1, y + 1, width - 2, BAR_HEIGHT - 2, FRAME_MID);

        context.fill(x + 3, y + 1, x + width - 3, y + 2, FRAME_LIGHT);
        context.fill(x + 1, y + 3, x + 2, y + BAR_HEIGHT - 3, 0xFF4B5660);
        context.fill(x + 3, y + BAR_HEIGHT - 2, x + width - 3, y + BAR_HEIGHT - 1, 0xFF171D23);

        int innerX = x + TRACK_INSET;
        int innerY = y + TRACK_INSET;
        int innerWidth = width - TRACK_INSET * 2;
        int innerHeight = BAR_HEIGHT - TRACK_INSET * 2;
        fillChamfered(context, innerX, innerY, innerWidth, innerHeight, TRACK_DARK);
        context.fill(innerX + 1, innerY + 1, innerX + innerWidth - 1, innerY + 2,
                0xFF141B22);
        context.fill(innerX + 1, innerY + innerHeight - 2,
                innerX + innerWidth - 1, innerY + innerHeight - 1, 0xFF05070A);

        int filledWidth = Math.round(innerWidth * ratio);
        if (ratio > 0.0F) {
            filledWidth = Math.max(1, filledWidth);
        }
        if (filledWidth > 0) {
            int clipRight = innerX + filledWidth;
            fillChamferedClipped(context, innerX, innerY, innerWidth, innerHeight,
                    0xFF000000 | fillColor, clipRight);
            fillChamferedClipped(context, innerX, innerY, innerWidth, 1,
                    0xFF000000 | mix(fillColor, 0xFFFFFF, 0.42F), clipRight);
            fillChamferedClipped(context, innerX, innerY + 1, innerWidth, 1,
                    0xFF000000 | mix(fillColor, 0xFFFFFF, 0.20F), clipRight);
            fillChamferedClipped(context, innerX, innerY + innerHeight - 2, innerWidth, 1,
                    0xFF000000 | mix(fillColor, 0x000000, 0.18F), clipRight);
            fillChamferedClipped(context, innerX, innerY + innerHeight - 1, innerWidth, 1,
                    0xFF000000 | mix(fillColor, 0x000000, 0.38F), clipRight);

            for (int textureX = innerX + 7; textureX < clipRight; textureX += 9) {
                context.fill(textureX, innerY + 2, textureX + 1,
                        innerY + innerHeight - 2, alphaColor(0.22F, mix(fillColor, 0x000000, 0.55F)));
                if (textureX + 1 < clipRight) {
                    context.fill(textureX + 1, innerY + 2, textureX + 2, innerY + 3,
                            alphaColor(0.72F, mix(fillColor, 0xFFFFFF, 0.48F)));
                }
            }

            if (filledWidth < innerWidth && filledWidth > 1) {
                int capX = clipRight - 1;
                context.fill(capX, innerY + 1, capX + 1, innerY + innerHeight - 1,
                        0xFF000000 | mix(fillColor, 0xFFFFFF, 0.26F));
            }
        }

        drawFrameNotches(context, x, y, width);

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        String text = RoundingUtil.longFormat(value);
        int textX = x + (width - renderer.getWidth(text)) / 2;
        context.drawText(renderer, text, textX, y + 2, TEXT_COLOR, true);
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
        int innerWidth = width - TRACK_INSET * 2;
        int dropX = Math.clamp(
                x + TRACK_INSET + Math.round(innerWidth * ratio) - 1,
                x + TRACK_INSET,
                x + width - TRACK_INSET - 1
        );
        int dropY = y + BAR_HEIGHT + Math.round(progress * 4.0F);
        int alpha = Math.round((1.0F - progress * 0.35F) * 255.0F);
        context.fill(dropX, dropY, dropX + 1, dropY + 2, (alpha << 24) | color);
    }

    private static void drawFrameNotches(DrawContext context, int x, int y, int width) {
        for (int section = 1; section < 4; section++) {
            int notchX = x + Math.round(width * (section / 4.0F));
            context.fill(notchX, y + 1, notchX + 1, y + 2, 0xFF1A2026);
            context.fill(notchX, y + BAR_HEIGHT - 2, notchX + 1, y + BAR_HEIGHT - 1, FRAME_LIGHT);
        }
    }

    private static void fillChamfered(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        fillChamferedClipped(context, x, y, width, height, color, x + width);
    }

    private static void fillChamferedClipped(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int color,
            int clipRight
    ) {
        for (int row = 0; row < height; row++) {
            int inset = row == 0 || row == height - 1 ? 1 : 0;
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
