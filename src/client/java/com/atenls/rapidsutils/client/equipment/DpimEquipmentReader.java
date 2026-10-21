package com.atenls.rapidsutils.client.equipment;

import com.atenls.rapidsutils.equipment.EquipmentSnapshot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DpimEquipmentReader {
    // Immutable components are replaced on slot updates. Do not parse JSON on every render frame.
    private static final Map<NbtComponent, Optional<EquipmentSnapshot>> CACHE = new LinkedHashMap<>();

    private DpimEquipmentReader() { }

    public static Optional<EquipmentSnapshot> read(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (stack.isEmpty() || component == null) {
            return Optional.empty();
        }
        Optional<EquipmentSnapshot> cached = CACHE.get(component);
        if (cached != null) {
            return cached;
        }
        Optional<EquipmentSnapshot> result = decode(component);
        if (CACHE.size() >= 64) {
            CACHE.clear();
        }
        CACHE.put(component, result);
        return result;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Optional<EquipmentSnapshot> decode(NbtComponent component) {
        NbtCompound pdc = component.copyNbt().getCompoundOrEmpty("PublicBukkitValues");
        if (pdc.getString("dpitemmanager:item_id", "").isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(EquipmentSnapshot.parse(pdc.getString("dpitemmanager:attrs", ""),
                    pdc.getString("dpitemmanager:affixes", "")));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
