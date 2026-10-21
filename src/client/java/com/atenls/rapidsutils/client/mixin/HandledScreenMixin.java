package com.atenls.rapidsutils.client.mixin;

import com.atenls.rapidsutils.client.equipment.EquipmentComparisonTooltip;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;
    @Unique private Runnable rapidsutils$previousDrawer;

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void rapidsutils$rememberDrawer(DrawContext context, int x, int y, CallbackInfo ci) {
        rapidsutils$previousDrawer = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer();
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
    private void rapidsutils$compare(DrawContext context, int x, int y, CallbackInfo ci) {
        Runnable drawer = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer();
        if (drawer != null && drawer != rapidsutils$previousDrawer) {
            EquipmentComparisonTooltip.append(context, focusedSlot);
        }
    }
}
