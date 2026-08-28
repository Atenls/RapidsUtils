package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.protocol.PayloadData;
import com.atenls.rapidsutils.text.MinecraftColorParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicTemplateFormatterTest {
    @Test
    void resolvesBuiltInsDataKeysAndRgbColors() {
        PayloadData data = new PayloadData.ObjectValue(Map.of(
                "dungeonDisplay", new PayloadData.ScalarValue(PayloadData.ScalarKind.STRING, "Frozen Vault"),
                "itemgot", new PayloadData.ScalarValue(PayloadData.ScalarKind.NUMBER, "3"),
                "itemgotmax", new PayloadData.ScalarValue(PayloadData.ScalarKind.NUMBER, "8")
        ));

        List<HudLine> lines = new TopicTemplateFormatter().format("""
                {rhombus} {dungeonDisplay}
                - &#999999材料掉落 &#80b0d0{itemgot}/{itemgotmax}
                """, data);

        assertEquals("◆ Frozen Vault", joined(lines.get(0)));
        assertEquals("- 材料掉落 3/8", joined(lines.get(1)));
        assertEquals(JsonDisplayFormatter.PRIMARY_TEXT, lines.get(1).spans().get(0).color());
        assertEquals(0x999999, lines.get(1).spans().get(1).color());
        assertEquals(0x80B0D0, lines.get(1).spans().get(2).color());
    }

    @Test
    void leavesUnknownVariablesVisibleForConfigurationFeedback() {
        PayloadData data = new PayloadData.ObjectValue(Map.of());

        List<HudLine> lines = new TopicTemplateFormatter().format("{missing}", data);

        assertEquals("{missing}", joined(lines.getFirst()));
    }

    private static String joined(HudLine line) {
        return line.spans().stream().map(MinecraftColorParser.Segment::text).reduce("", String::concat);
    }
}
