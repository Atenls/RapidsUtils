package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.text.MinecraftColorParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HudLineTest {
    @Test
    void appendsGrayCountWithoutChangingOriginalContentOrIndent() {
        HudLine original = new HudLine(2, List.of(new MinecraftColorParser.Segment("获得物品", 0x55FF55)));

        HudLine counted = original.withRepeatCount(3L);

        assertEquals(2, counted.depth());
        assertEquals(List.of(
                new MinecraftColorParser.Segment("获得物品", 0x55FF55),
                new MinecraftColorParser.Segment(" (x3)", 0xAAAAAA)
        ), counted.spans());
        assertEquals(1, original.spans().size());
        assertEquals(" (x" + Long.MAX_VALUE + ")", original.withRepeatCount(Long.MAX_VALUE).spans().getLast().text());
    }

    @Test
    void leavesSingleMessageWithoutSuffix() {
        HudLine line = new HudLine(0, List.of(new MinecraftColorParser.Segment("消息", 0xFFFFFF)));

        assertSame(line, line.withRepeatCount(1L));
    }
}
