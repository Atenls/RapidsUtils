package com.atenls.rapidsutils.equipment;

/** Coordinates for two native tooltips, including room for their backgrounds. */
public record ComparisonLayout(int x, int y, float scale, int primaryX, int comparisonX, int viewportHeight) {
    public static final int GAP = 12;
    private static final int MARGIN = 12;

    public static ComparisonLayout place(int screenWidth, int screenHeight, int preferredX, int preferredY,
                                         int primaryWidth, int primaryHeight, int comparisonWidth, int comparisonHeight) {
        int totalWidth = primaryWidth + GAP + comparisonWidth;
        int viewportHeight = Math.min(Math.max(primaryHeight, comparisonHeight), Math.max(1, screenHeight - 2 * MARGIN));
        // Keep the primary tooltip at its vanilla anchor whenever either side fits.
        boolean onRight = totalWidth > screenWidth - 2 * MARGIN
                || preferredX + totalWidth <= screenWidth - MARGIN
                || preferredX - (comparisonWidth + GAP) < MARGIN;
        int primaryX = onRight ? 0 : comparisonWidth + GAP;
        int comparisonX = onRight ? primaryWidth + GAP : 0;
        int x = Math.max(MARGIN, Math.min(preferredX - primaryX, screenWidth - MARGIN - totalWidth));
        int y = Math.max(MARGIN, Math.min(preferredY, screenHeight - MARGIN - viewportHeight));
        return new ComparisonLayout(x, y, 1F, primaryX, comparisonX, viewportHeight);
    }
}
