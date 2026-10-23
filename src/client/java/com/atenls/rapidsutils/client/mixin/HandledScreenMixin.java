package com.atenls.rapidsutils.client.mixin;

import com.atenls.rapidsutils.client.equipment.EquipmentComparisonTooltip;
import com.atenls.rapidsutils.client.render.ItemSlotTintRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;
    @Unique private Runnable rapidsutils$previousDrawer;
    @Unique private final EquipmentComparisonTooltip.ScrollState rapidsutils$scroll = new EquipmentComparisonTooltip.ScrollState();

    @Shadow
    private Slot getSlotAt(double x, double y) {
        throw new AssertionError();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void rapidsutils$scrollTooltip(double mouseX, double mouseY, double horizontal, double vertical,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (rapidsutils$scroll.scroll(getSlotAt(mouseX, mouseY), vertical)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void rapidsutils$clearTooltipScroll(CallbackInfo ci) {
        rapidsutils$scroll.clear();
    }

    @Inject(method = "drawSlots", at = @At("HEAD"))
    private void rapidsutils$tintSlots(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        ItemSlotTintRenderer.drawSlots(context, ((HandledScreen<?>) (Object) this).getScreenHandler().slots);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void rapidsutils$rememberDrawer(DrawContext context, int x, int y, CallbackInfo ci) {
        rapidsutils$scroll.beginFrame(focusedSlot);
        rapidsutils$previousDrawer = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer();
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
    private void rapidsutils$compare(DrawContext context, int x, int y, CallbackInfo ci) {
        Runnable drawer = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer();
        if (drawer != null && drawer != rapidsutils$previousDrawer) {
            EquipmentComparisonTooltip.append(context, focusedSlot, rapidsutils$scroll);
        }
    }
}
