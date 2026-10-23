package com.atenls.rapidsutils.equipment;

/** Shared wheel travel, with each panel stopping at its own bottom edge. */
public final class ComparisonScroll {
    private static final int PIXELS_PER_NOTCH = 18;
    private double position;
    private int maximum;

    public void configure(int primaryHeight, int comparisonHeight, int viewportHeight) {
        maximum = Math.max(0, Math.max(primaryHeight, comparisonHeight) - viewportHeight);
        position = Math.min(position, maximum);
    }

    public boolean scroll(double vertical) {
        if (maximum == 0 || vertical == 0 || !Double.isFinite(vertical)) {
            return false;
        }
        position = Math.max(0, Math.min(maximum, position - vertical * PIXELS_PER_NOTCH));
        // Consume at the limits too, so the same gesture cannot trigger inventory actions.
        return true;
    }

    public int offset(int contentHeight, int viewportHeight) {
        return Math.min((int) Math.round(position), Math.max(0, contentHeight - viewportHeight));
    }

    public void reset() {
        position = 0;
        maximum = 0;
    }
}
