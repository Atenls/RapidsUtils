package com.atenls.rapidsutils.client.mixin;

import com.atenls.rapidsutils.client.equipment.TooltipScrollHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    // malilib 0.27.12 dispatches Item Scroller AFTER this call. Run BEFORE it, after
    // vanilla has validated the window and recorded input activity, without targeting mod internals.
    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;"),
            cancellable = true)
    private void rapidsutils$scrollTooltip(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.getOverlay() != null || !(client.currentScreen instanceof TooltipScrollHandler handler)) {
            return;
        }
        Mouse mouse = (Mouse) (Object) this;
        double amount = (client.options.getDiscreteMouseScroll().getValue() ? Math.signum(vertical) : vertical)
                * client.options.getMouseWheelSensitivity().getValue();
        if (handler.rapidsutils$scrollTooltip(mouse.getScaledX(client.getWindow()), mouse.getScaledY(client.getWindow()), amount)) {
            client.currentScreen.applyMousePressScrollNarratorDelay();
            ci.cancel();
        }
    }
}
