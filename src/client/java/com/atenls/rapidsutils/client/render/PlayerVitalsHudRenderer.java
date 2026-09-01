package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.client.config.RapidsConfig;
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
    private static final float LOW_HEALTH_THRESHOLD = 0.25F;
    private static final int HEALTH_CRITICAL = 0x77131F;
    private static final int HEALTH_HEALTHY = 0xDD4655;
    private static final int MANA_BLUE = 0x2F91D2;
    private static final int TEXT_COLOR = 0xFFFFF8EC;
    private static final int TRACK = 0xA3080B0F;
    private static final int TRACK_SOFT = 0x6B090D12;
    private static final int FRAME = 0x7ACFDADC;
    private static final int SILVER = 0xFFC9D9DC;
    private static final int[] LOW_HEALTH_SHAKE_X = {0, -1, 1, -1, 1, 0, 0, 0, 0};
    private static final int[] LOW_HEALTH_SHAKE_Y = {0, 0, 0, 1, 0, 0, 0, 0, 0};

    private final PlayerVitalsState state;
    private final RapidsConfig config;

    public PlayerVitalsHudRenderer(PlayerVitalsState state, RapidsConfig config) {
        this.state = state;
        this.config = config;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        state.snapshot().ifPresent(vitals -> renderBars(context, tickCounter, vitals));
    }

    private void renderBars(DrawContext context, RenderTickCounter tickCounter, PlayerVitals vitals) {
        int groupWidth = Math.min(GROUP_WIDTH, context.getScaledWindowWidth() - 12);
        int barWidth = Math.round(groupWidth * BAR_WIDTH_RATIO);
        if (barWidth < 48) {
            return;
        }

        RapidsConfig.VitalsBarStyle style = config.vitalsBarStyle;
        int groupX = (context.getScaledWindowWidth() - groupWidth) / 2;
        int y = context.getScaledWindowHeight()
                - HudStatusBarHeightRegistry.getHeight(ID)
                - style.height();
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
                healthColor,
                style
        );
        if (motion.dropProgress() >= 0.0F) {
            drawBloodDrop(
                    context,
                    groupX + motion.x(),
                    y + motion.y(),
                    barWidth,
                    healthRatio,
                    healthColor,
                    motion.dropProgress(),
                    style
            );
        }
        drawBar(
                context,
                groupX + groupWidth - barWidth,
                y,
                barWidth,
                vitals.mana(),
                vitals.manaRatio(),
                MANA_BLUE,
                style
        );
    }

    private static void drawBar(
            DrawContext context,
            int x,
            int y,
            int width,
            BigDecimal value,
            float ratio,
            int fillColor,
            RapidsConfig.VitalsBarStyle style
    ) {
        switch (style) {
            case A -> drawGlassBar(context, x, y, width, ratio, fillColor);
            case B -> drawHairlineBar(context, x, y, width, ratio, fillColor);
            case C -> drawFloatingBar(context, x, y, width, ratio, fillColor);
            case E -> drawSilverBar(context, x, y, width, ratio, fillColor);
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        String text = RoundingUtil.longFormat(value);
        int textX = x + (width - renderer.getWidth(text)) / 2;
        int textY = style.height() == 11 ? y + 2 : y;
        context.drawText(renderer, text, textX, textY, TEXT_COLOR, true);
    }

    private static void drawGlassBar(
            DrawContext context,
            int x,
            int y,
            int width,
            float ratio,
            int color
    ) {
        int filledWidth = filledWidth(width, ratio);
        context.fill(x, y + 1, x + width, y + 9, TRACK_SOFT);
        context.fill(x, y + 1, x + filledWidth, y + 9, 0xFF000000 | color);
        if (filledWidth > 1) {
            context.fill(x + 1, y + 1, x + filledWidth, y + 2,
                    0xFF000000 | mix(color, 0xFFFFFF, 0.42F));
        }
        context.fill(x, y + 8, x + filledWidth, y + 9,
                0xFF000000 | mix(color, 0x000000, 0.42F));
    }

    private static void drawHairlineBar(
            DrawContext context,
            int x,
            int y,
            int width,
            float ratio,
            int color
    ) {
        context.fill(x + 1, y, x + width - 1, y + 1, FRAME);
        context.fill(x, y + 1, x + 1, y + 10, FRAME);
        context.fill(x + width - 1, y + 1, x + width, y + 10, FRAME);
        context.fill(x + 1, y + 10, x + width - 1, y + 11, FRAME);

        int innerWidth = width - 2;
        int filledWidth = filledWidth(innerWidth, ratio);
        context.fill(x + 1, y + 1, x + width - 1, y + 10, TRACK);
        context.fill(x + 1, y + 1, x + 1 + filledWidth, y + 10, 0xFF000000 | color);
        if (filledWidth > 1) {
            context.fill(x + 2, y + 1, x + 1 + filledWidth, y + 2,
                    0xFF000000 | mix(color, 0xFFFFFF, 0.42F));
        }
    }

    private static void drawFloatingBar(
            DrawContext context,
            int x,
            int y,
            int width,
            float ratio,
            int color
    ) {
        int filledWidth = filledWidth(width, ratio);
        context.fill(x, y + 4, x + width, y + 7, TRACK_SOFT);
        context.fill(x, y + 3, x + filledWidth, y + 8, 0xFF000000 | color);
        if (filledWidth > 1) {
            context.fill(x + 1, y + 3, x + filledWidth, y + 4,
                    0xFF000000 | mix(color, 0xFFFFFF, 0.42F));
        }
        context.fill(x, y + 7, x + filledWidth, y + 8,
                0xFF000000 | mix(color, 0x000000, 0.42F));
        int capX = x + filledWidth - 1;
        context.fill(capX, y + 2, capX + 1, y + 9,
                0xFF000000 | mix(color, 0xFFFFFF, 0.32F));
    }

    private static void drawSilverBar(
            DrawContext context,
            int x,
            int y,
            int width,
            float ratio,
            int color
    ) {
        int innerWidth = width - 4;
        int filledWidth = filledWidth(innerWidth, ratio);
        context.fill(x + 2, y + 1, x + width - 2, y + 10, TRACK_SOFT);
        context.fill(x + 2, y + 1, x + 2 + filledWidth, y + 10, 0xFF000000 | color);
        if (filledWidth > 1) {
            context.fill(x + 3, y + 1, x + 2 + filledWidth, y + 2,
                    0xFF000000 | mix(color, 0xFFFFFF, 0.42F));
        }

        context.fill(x, y + 2, x + 1, y + 9, SILVER);
        context.fill(x + 1, y + 1, x + 2, y + 10, SILVER);
        context.fill(x + width - 2, y + 1, x + width - 1, y + 10, SILVER);
        context.fill(x + width - 1, y + 2, x + width, y + 9, SILVER);
        int centerX = x + width / 2;
        context.fill(centerX, y, centerX + 1, y + 2, SILVER);
        context.fill(centerX, y + 9, centerX + 1, y + 11, SILVER);
    }

    private static int filledWidth(int width, float ratio) {
        int filledWidth = Math.round(width * ratio);
        return ratio > 0.0F ? Math.max(1, filledWidth) : 0;
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
            float progress,
            RapidsConfig.VitalsBarStyle style
    ) {
        int inset = switch (style) {
            case B -> 1;
            case E -> 2;
            case A, C -> 0;
        };
        int innerWidth = width - inset * 2;
        int dropX = Math.clamp(
                x + inset + Math.round(innerWidth * ratio) - 1,
                x + inset,
                x + width - inset - 1
        );
        int dropY = y + style.height() + Math.round(progress * 4.0F);
        int alpha = Math.round((1.0F - progress * 0.35F) * 255.0F);
        context.fill(dropX, dropY, dropX + 1, dropY + 2, (alpha << 24) | color);
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
