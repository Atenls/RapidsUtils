package com.atenls.rapidsutils.equipment;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SlotTintColorsTest {
    private SlotTintColors palette() {
        return SlotTintColors.empty().withOverrides(new InputStreamReader(
                getClass().getResourceAsStream("/assets/rapidsutils/quality_colors.json"), StandardCharsets.UTF_8));
    }

    @Test
    void usesDpimQualityColorAndSupportsStarredTag() {
        var colors = palette();
        assertEquals(0xCC80CC, colors.resolve("{\"quality-name\":\"史诗\"}").qualityColor().orElseThrow());
        assertEquals(0xFFAA00, colors.resolve("{\"quality-name*\":\"传奇\",\"quality-name\":\"普通\"}")
                .qualityColor().orElseThrow());
        assertEquals(0x65CF65, colors.resolve("{\"quality-name\":\"  &#65CF65优秀  \"}")
                .qualityColor().orElseThrow());
    }

    @Test
    void onlyItemsWithValidTagsAreEligibleForNameFallback() {
        var colors = palette();
        for (String missing : new String[]{null, "", "null", "[]", "broken", "{", " ".repeat(262_145)}) {
            assertFalse(colors.resolve(missing).eligible());
        }
        for (String tags : new String[]{"{}", "{\"material\":\"stone\"}", "{\"quality-name\":\"未知品质\"}"}) {
            var result = colors.resolve(tags);
            assertTrue(result.eligible());
            assertTrue(result.qualityColor().isEmpty());
        }
    }

    @Test
    void overridesOneQualityWithoutDroppingOtherQualities() {
        var colors = palette().withOverrides(new StringReader("{\"史诗\":\"#123456\"}"));
        assertEquals(0x123456, colors.resolve("{\"quality-name\":\"史诗\"}").qualityColor().orElseThrow());
        assertEquals(0xDDDDDD, colors.resolve("{\"quality-name\":\"普通\"}").qualityColor().orElseThrow());
    }
}
