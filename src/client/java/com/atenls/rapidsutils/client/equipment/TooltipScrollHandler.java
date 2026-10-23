package com.atenls.rapidsutils.client.equipment;

/** Lets the mouse dispatch tooltip scrolling before inventory-management mods see the gesture. */
public interface TooltipScrollHandler {
    boolean rapidsutils$scrollTooltip(double mouseX, double mouseY, double vertical);
}
