package com.atenls.rapidsutils.client.render;

import com.atenls.rapidsutils.util.VitalsFontMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Vitals-only smooth textures; does not alter Minecraft's nearest-sampled fonts. */
final class VitalsTextRenderer {
    private static final Identifier[] ATLASES = new Identifier[17];
    static {
        for (int i = 1; i <= 16; i++) {
            if (i <= 8 || i == 16) {
                ATLASES[i] = Identifier.of("rapidsutils", "textures/font/vitals_" + i + "x.png");
            }
        }
    }

    private VitalsTextRenderer() {}

    static void draw(DrawContext context, String text, int x, int y, int width, int height, int color) {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int density = VitalsFontMetrics.density(guiScale);
        int cell = VitalsFontMetrics.CELL;
        float cursor = x + (width - VitalsFontMetrics.width(text)) / 2.0F;
        float top = y + (height - VitalsFontMetrics.CAP_HEIGHT) / 2.0F;
        var matrices = context.getMatrices();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int slot = VitalsFontMetrics.glyphIndex(character);
            if (character != ' ') {
                matrices.pushMatrix();
                try {
                    // Snap origins to physical pixels, not whole GUI pixels.
                    matrices.translate((float) (Math.round(cursor * guiScale) / guiScale) - VitalsFontMetrics.PADDING,
                            (float) (Math.round(top * guiScale) / guiScale) - VitalsFontMetrics.PADDING);
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, ATLASES[density], 0, 0,
                            slot % 16 * cell * density, slot / 16 * cell * density,
                            cell, cell, cell * density, cell * density,
                            16 * cell * density, 6 * cell * density, color);
                } finally {
                    matrices.popMatrix();
                }
            }
            cursor += VitalsFontMetrics.advance(character);
        }
    }
}
