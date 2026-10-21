package com.atenls.rapidsutils.client.mixin;

import com.atenls.rapidsutils.client.equipment.EquipmentComparisonTooltip;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Inject(method = "drawTooltipImmediately", at = @At("HEAD"), cancellable = true)
    private void rapidsutils$drawComparison(TextRenderer font, List<TooltipComponent> primary, int x, int y,
                                           TooltipPositioner positioner, Identifier style, CallbackInfo ci) {
        if (EquipmentComparisonTooltip.renderPending((DrawContext) (Object) this, font, primary, x, y, positioner, style)) {
            ci.cancel();
        }
    }
}
