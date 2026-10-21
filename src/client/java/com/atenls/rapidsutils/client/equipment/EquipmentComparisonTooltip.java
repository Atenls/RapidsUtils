package com.atenls.rapidsutils.client.equipment;

import com.atenls.rapidsutils.client.config.RapidsConfig;
import com.atenls.rapidsutils.client.mixin.DrawContextAccessor;
import com.atenls.rapidsutils.equipment.ComparisonLayout;
import com.atenls.rapidsutils.equipment.EquipmentComparison;
import com.atenls.rapidsutils.equipment.EquipmentSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EquipmentComparisonTooltip {
    private EquipmentComparisonTooltip() { }

    public static void draw(DrawContext context, TextRenderer font, List<Text> primary,
                            Optional<TooltipData> data, int mouseX, int mouseY, Identifier style, Slot hovered) {
        // Preserve vanilla's deferred tooltip behavior, including when no comparison is available.
        boolean alreadyQueued = ((DrawContextAccessor) context).rapidsutils$getTooltipDrawer() != null;
        context.drawTooltip(font, primary, data, mouseX, mouseY, style);
        MinecraftClient client = MinecraftClient.getInstance();
        if (alreadyQueued || (primary.isEmpty() && data.isEmpty()) || !RapidsConfig.get().equipmentComparison
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
        comparison.add(Text.translatable("tooltip.rapidsutils.compare.title").formatted(Formatting.GOLD));
        comparison.add(Text.translatable("tooltip.rapidsutils.compare.slot." + slot.getName()).formatted(Formatting.GRAY));
        comparison.add(equipped.isEmpty() ? Text.translatable("tooltip.rapidsutils.compare.empty").formatted(Formatting.GRAY)
                : Text.translatable("tooltip.rapidsutils.compare.equipped", equipped.getName()));
        if (!equipped.isEmpty() && old.isEmpty()) {
            comparison.add(Text.translatable("tooltip.rapidsutils.compare.no_attributes").formatted(Formatting.DARK_GRAY));
        }
        comparison.add(Text.empty());
        for (var row : rows) {
            var display = EquipmentAttributeDisplays.get(row.key().name(), row.displayName());
            boolean percent = row.key().percent() || display.percent() || row.key().name().endsWith("_PERCENT");
            String unit = percent ? "%" : "";
            var line = Text.literal(display.icon() + display.name()).withColor(display.color());
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
        comparison.add(Text.translatable("tooltip.rapidsutils.compare.no_sets").formatted(Formatting.DARK_GRAY));
        List<TooltipComponent> main = components(primary);
        data.ifPresent(value -> main.add(main.isEmpty() ? 0 : 1, TooltipComponent.of(value)));
        List<TooltipComponent> side = components(comparison);
        // DrawContext holds one deferred drawer. Queue both panels together so neither replaces the other.
        ((DrawContextAccessor) context).rapidsutils$setTooltipDrawer(() -> {
            int mainWidth = width(main, font);
            int mainHeight = height(main, font);
            var anchor = HoveredTooltipPositioner.INSTANCE.getPosition(context.getScaledWindowWidth(),
                    context.getScaledWindowHeight(), mouseX, mouseY, mainWidth, mainHeight);
            var layout = ComparisonLayout.place(context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                    anchor.x(), anchor.y(), mainWidth, mainHeight, width(side, font), height(side, font));
            context.getMatrices().pushMatrix();
            try {
                context.getMatrices().translate(layout.x(), layout.y());
                context.getMatrices().scale(layout.scale());
                context.drawTooltipImmediately(font, main, mouseX, mouseY,
                        (sw, sh, mx, my, w, h) -> new Vector2i(layout.primaryX(), 0), style);
                context.drawTooltipImmediately(font, side, mouseX, mouseY,
                        (sw, sh, mx, my, w, h) -> new Vector2i(layout.comparisonX(), 0), null);
            } finally {
                context.getMatrices().popMatrix();
            }
        });
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
