package com.atenls.rapidsutils.client.equipment;

import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.client.mixin.DrawContextAccessor;
import com.atenls.rapidsutils.equipment.ComparisonLayout;
import com.atenls.rapidsutils.equipment.ComparisonScroll;
import com.atenls.rapidsutils.equipment.EquipmentComparison;
import com.atenls.rapidsutils.equipment.EquipmentSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EquipmentComparisonTooltip {
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    private EquipmentComparisonTooltip() { }

    public static void append(DrawContext context, Slot hovered, ScrollState scroll) {
        Runnable original = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer();
        MinecraftClient client = MinecraftClient.getInstance();
        if (original == null || !RapidsConfig.get().equipmentComparison
                || client.player == null || hovered == null
                || hovered.inventory != client.player.getInventory()) {
            return;
        }
        ItemStack candidate = hovered.getStack();
        EquipmentSlot slot = slot(candidate);
        if (slot == null) {
            return;
        }
        ItemStack equipped = client.player.getEquippedStack(slot);
        if (candidate == equipped) {
            return;
        }
        Optional<EquipmentSnapshot> next = DpimEquipmentReader.read(candidate);
        Optional<EquipmentSnapshot> old = DpimEquipmentReader.read(equipped);
        // A malformed managed baseline must never silently become a zero-attribute item.
        if (next.isEmpty() || (!equipped.isEmpty() && old.isEmpty() && isManaged(equipped))) {
            return;
        }
        List<EquipmentComparison.Row> rows = EquipmentComparison.compare(next.get(), old.orElse(EquipmentSnapshot.empty()));
        if (rows.isEmpty()) {
            return;
        }
        List<Text> comparison = new ArrayList<>();
        comparison.add(Text.translatable("tooltip.rapidsutils.compare.slot." + slot.getName()).formatted(Formatting.GRAY));
        comparison.add(Text.literal(" - ").formatted(Formatting.DARK_GRAY).append((equipped.isEmpty()
                ? Text.translatable("tooltip.rapidsutils.compare.empty")
                : Text.translatable("tooltip.rapidsutils.compare.equipped", equipped.getName())).formatted(Formatting.GRAY)));
        if (!equipped.isEmpty() && old.isEmpty()) {
            comparison.add(Text.translatable("tooltip.rapidsutils.compare.no_attributes").formatted(Formatting.DARK_GRAY));
        }
        comparison.add(Text.empty());
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            var display = EquipmentAttributeDisplays.get(row.key().name(), row.displayName());
            boolean percent = row.key().percent() || display.percent() || row.key().name().endsWith("_PERCENT");
            String unit = percent ? "%" : "";
            String branch = rows.size() == 1 ? "-" : index == 0 ? "┌" : index == rows.size() - 1 ? "└" : "├";
            var line = Text.literal("  " + branch + " ").formatted(Formatting.DARK_GRAY);
            line.append(Text.literal(display.icon() + display.name()).withColor(display.color()));
            line.append(Text.literal(" " + range(row.before().min(), row.before().max()) + unit + " → "
                    + range(row.after().min(), row.after().max()) + unit).formatted(Formatting.WHITE));
            if (!display.suffix().isBlank()) {
                line.append(Text.literal(plainSuffix(display.suffix())).formatted(Formatting.GRAY));
            }
            String delta = row.minDelta().compareTo(row.maxDelta()) == 0 ? signed(row.minDelta())
                    : signed(row.minDelta()) + " ~ " + signed(row.maxDelta());
            int direction = row.direction(display.lowerIsBetter());
            line.append(Text.literal(" (" + delta + unit + ")").formatted(direction > 0 ? Formatting.GREEN
                    : direction < 0 ? Formatting.RED : Formatting.GRAY));
            comparison.add(line);
        }
        comparison.add(Text.empty());
        List<TooltipComponent> side = components(comparison);
        scroll.track(candidate, equipped);
        // Keep the exact drawer installed by vanilla and other mods, including its components and positioner.
        ((DrawContextAccessor) context).rapidsutils$setTooltipDrawer(() -> {
            PENDING.set(new Pending(context, side, scroll));
            try {
                original.run();
            } finally {
                PENDING.remove();
            }
        });
    }

    public static boolean renderPending(DrawContext context, TextRenderer font, List<TooltipComponent> main,
                                         int mouseX, int mouseY, TooltipPositioner positioner, Identifier style) {
        Pending pending = PENDING.get();
        if (pending == null || pending.context() != context || main.isEmpty()) {
            return false;
        }
        // Consume before calling native drawing again, preventing recursive interception.
        PENDING.remove();
        List<TooltipComponent> side = pending.side();
        int mainWidth = width(main, font);
        int mainHeight = height(main, font);
        int sideWidth = width(side, font);
        int sideHeight = height(side, font);
        var anchor = positioner.getPosition(context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                mouseX, mouseY, mainWidth, mainHeight);
        var layout = ComparisonLayout.place(context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                anchor.x(), anchor.y(), mainWidth, mainHeight, sideWidth, sideHeight);
        ScrollState scroll = pending.scroll();
        scroll.travel.configure(mainHeight, sideHeight, layout.viewportHeight());
        scroll.rendered = true;
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(layout.x(), layout.y());
            // Native backgrounds extend 12 pixels past the text bounds. Draw both before any
            // foreground so a neighboring background cannot darken already rendered glyphs.
            TooltipBackgroundRenderer.render(context, layout.primaryX(), 0, mainWidth,
                    Math.min(mainHeight, layout.viewportHeight()), style);
            TooltipBackgroundRenderer.render(context, layout.comparisonX(), 0, sideWidth,
                    Math.min(sideHeight, layout.viewportHeight()), null);
            context.createNewRootLayer();
            drawPanel(context, font, main, layout.primaryX(), mainWidth, mainHeight, layout.viewportHeight(), scroll.travel);
            drawPanel(context, font, side, layout.comparisonX(), sideWidth, sideHeight, layout.viewportHeight(), scroll.travel);
        } finally {
            context.getMatrices().popMatrix();
        }
        return true;
    }

    private static void drawPanel(DrawContext context, TextRenderer font, List<TooltipComponent> lines,
                                  int x, int width, int height, int viewportHeight, ComparisonScroll scroll) {
        if (height <= viewportHeight) {
            drawContents(context, font, lines, x, width, height);
            return;
        }
        int offset = scroll.offset(height, viewportHeight);
        // Establish the viewport before translating the content; DrawContext transforms scissors.
        context.enableScissor(x - 2, 0, x + width + 2, viewportHeight + 2);
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(0, -offset);
            drawContents(context, font, lines, x, width, height);
        } finally {
            context.getMatrices().popMatrix();
            context.disableScissor();
        }
        if (offset > 0) {
            drawScrollArrow(context, x, width, -8, true);
        }
        if (offset < height - viewportHeight) {
            drawScrollArrow(context, x, width, viewportHeight + 5, false);
        }
    }

    private static void drawScrollArrow(DrawContext context, int x, int width, int y, boolean up) {
        // A seven-pixel chevron with a soft shadow and tapered rules, inside the native padding.
        int center = x + width / 2;
        int ruleLength = Math.max(0, Math.min(11, width / 2 - 8));
        for (int i = 0; i < ruleLength; i++) {
            int alpha = 32 + (ruleLength - i) * 5;
            int color = (alpha << 24) | 0x9283B0;
            context.fill(center - 7 - i, y + 2, center - 6 - i, y + 3, color);
            context.fill(center + 6 + i, y + 2, center + 7 + i, y + 3, color);
        }
        for (int i = 0; i < 4; i++) {
            int row = y + (up ? i : 3 - i);
            int color = i == 0 ? 0xFFF0E8FF : i == 3 ? 0xFF8E829F : 0xFFC2B6D4;
            context.fill(center - i, row + 1, center - i + 1, row + 2, 0xB010081C);
            context.fill(center + i, row + 1, center + i + 1, row + 2, 0xB010081C);
            context.fill(center - i, row, center - i + 1, row + 1, color);
            context.fill(center + i, row, center + i + 1, row + 1, color);
        }
    }

    private static void drawContents(DrawContext context, TextRenderer font, List<TooltipComponent> lines,
                                     int x, int width, int height) {
        // Match vanilla's text pass, item pass, and two-pixel spacing after the first component.
        int y = 0;
        for (int index = 0; index < lines.size(); index++) {
            TooltipComponent line = lines.get(index);
            line.drawText(context, font, x, y);
            y += line.getHeight(font) + (index == 0 ? 2 : 0);
        }
        y = 0;
        for (int index = 0; index < lines.size(); index++) {
            TooltipComponent line = lines.get(index);
            line.drawItems(font, x, y, width, height, context);
            y += line.getHeight(font) + (index == 0 ? 2 : 0);
        }
    }

    private record Pending(DrawContext context, List<TooltipComponent> side, ScrollState scroll) { }

    /** Owned by one inventory screen, so offsets cannot leak between screens or players. */
    public static final class ScrollState {
        private final ComparisonScroll travel = new ComparisonScroll();
        private Slot hovered;
        private ItemStack candidate = ItemStack.EMPTY;
        private ItemStack equipped = ItemStack.EMPTY;
        private boolean rendered;

        public void beginFrame(Slot slot) {
            rendered = false;
            if (hovered != slot) {
                clear();
                hovered = slot;
            }
        }

        private void track(ItemStack next, ItemStack old) {
            if (!ItemStack.areEqual(candidate, next) || !ItemStack.areEqual(equipped, old)) {
                travel.reset();
                candidate = next.copy();
                equipped = old.copy();
            }
        }

        public boolean scroll(Slot slot, double vertical) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!rendered || slot == null || slot != hovered || !RapidsConfig.get().equipmentComparison
                    || !ItemStack.areEqual(candidate, slot.getStack())
                    || !(InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT))) {
                return false;
            }
            return travel.scroll(vertical);
        }

        public void clear() {
            travel.reset();
            hovered = null;
            candidate = ItemStack.EMPTY;
            equipped = ItemStack.EMPTY;
            rendered = false;
        }
    }

    private static EquipmentSlot slot(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && switch (equippable.slot()) {
            case HEAD, CHEST, LEGS, FEET, OFFHAND -> true;
            default -> false;
        }) {
            return equippable.slot();
        }
        if (stack.isOf(Items.SHIELD)) {
            return EquipmentSlot.OFFHAND;
        }
        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isIn(ItemTags.SPEARS)
                || stack.isOf(Items.BOW)
                || stack.isOf(Items.CROSSBOW) || stack.isOf(Items.TRIDENT) || stack.isOf(Items.MACE)
                || hasWeaponTag(stack)) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    private static boolean hasWeaponTag(ItemStack stack) {
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        String tags = custom.copyNbt().getCompoundOrEmpty("PublicBukkitValues").getString("dpitemmanager:tags", "");
        if (tags.length() > 262_144) {
            return false;
        }
        try {
            var object = com.google.gson.JsonParser.parseString(tags).getAsJsonObject();
            return object.has("weapon-type") && !object.get("weapon-type").getAsString().isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean isManaged(ItemStack stack) {
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        return custom != null && !custom.copyNbt().getCompoundOrEmpty("PublicBukkitValues")
                .getString("dpitemmanager:item_id", "").isBlank();
    }

    private static String range(BigDecimal min, BigDecimal max) {
        return min.compareTo(max) == 0 ? EquipmentComparison.format(min)
                : EquipmentComparison.format(min) + " ~ " + EquipmentComparison.format(max);
    }

    private static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + EquipmentComparison.format(value);
    }

    private static String plainSuffix(String value) {
        return value.replaceAll("(?i)[&§][0-9a-fk-or]|&?#?[<]#[0-9a-f]{6}[>]|&#[0-9a-f]{6}", "");
    }

    private static List<TooltipComponent> components(List<Text> lines) {
        return new ArrayList<>(lines.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList());
    }

    private static int width(List<TooltipComponent> lines, TextRenderer font) {
        return lines.stream().mapToInt(line -> line.getWidth(font)).max().orElse(0);
    }

    private static int height(List<TooltipComponent> lines, TextRenderer font) {
        return lines.stream().mapToInt(line -> line.getHeight(font)).sum() + (lines.size() == 1 ? -2 : 0);
    }
}
