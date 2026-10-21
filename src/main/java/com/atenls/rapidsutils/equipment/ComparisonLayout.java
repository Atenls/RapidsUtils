package com.atenls.rapidsutils.equipment;

/** Coordinates for two native tooltips, including room for their backgrounds. */
public record ComparisonLayout(int x, int y, float scale, int primaryX, int comparisonX) {
    public static final int GAP = 8;
    private static final int MARGIN = 12;

    public static ComparisonLayout place(int screenWidth, int screenHeight, int preferredX, int preferredY,
                                         int primaryWidth, int primaryHeight, int comparisonWidth, int comparisonHeight) {
        int totalWidth = primaryWidth + GAP + comparisonWidth;
        int totalHeight = Math.max(primaryHeight, comparisonHeight) + 2;
        float scale = Math.min(1F, Math.min(Math.max(1, screenWidth - 2 * MARGIN) / (float) totalWidth,
                Math.max(1, screenHeight - 2 * MARGIN) / (float) totalHeight));
        int scaledWidth = (int) Math.ceil(totalWidth * scale);
        int scaledHeight = (int) Math.ceil(totalHeight * scale);
        // Keep the primary tooltip at its vanilla anchor whenever either side fits.
        boolean onRight = preferredX + scaledWidth <= screenWidth - MARGIN
                || preferredX - (comparisonWidth + GAP) * scale < MARGIN;
        int primaryX = onRight ? 0 : comparisonWidth + GAP;
        int comparisonX = onRight ? primaryWidth + GAP : 0;
        int x = Math.round(preferredX - primaryX * scale);
        x = Math.max(MARGIN, Math.min(x, screenWidth - MARGIN - scaledWidth));
        int y = Math.max(MARGIN, Math.min(preferredY, screenHeight - MARGIN - scaledHeight));
        return new ComparisonLayout(x, y, scale, primaryX, comparisonX);
    }
}
