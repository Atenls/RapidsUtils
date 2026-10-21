package com.atenls.rapidsutils.client.mixin;

import com.atenls.rapidsutils.client.equipment.EquipmentComparisonTooltip;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;

    @Redirect(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V"))
    private void rapidsutils$compare(DrawContext context, TextRenderer font, List<Text> lines,
                                     Optional<TooltipData> data, int x, int y, Identifier style) {
        EquipmentComparisonTooltip.draw(context, font, lines, data, x, y, style, focusedSlot);
    }
}
