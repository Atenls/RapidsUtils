package com.atenls.rapidsutils.client.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {
    @Accessor("tooltipDrawer")
    Runnable rapidsutils$getTooltipDrawer();

    @Accessor("tooltipDrawer")
    void rapidsutils$setTooltipDrawer(Runnable drawer);
}
