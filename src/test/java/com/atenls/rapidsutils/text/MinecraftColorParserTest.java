package com.atenls.rapidsutils.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftColorParserTest {
    @Test
    void parsesLegacyHexAndResetColors() {
        List<MinecraftColorParser.Segment> segments = MinecraftColorParser.parse(
                "plain §aGreen §x§1§2§A§b§F§0Hex§r reset",
                0xAABBCC,
                100
        );

        assertEquals(List.of(
                new MinecraftColorParser.Segment("plain ", 0xAABBCC),
                new MinecraftColorParser.Segment("Green ", 0x55FF55),
                new MinecraftColorParser.Segment("Hex", 0x12ABF0),
                new MinecraftColorParser.Segment(" reset", 0xAABBCC)
        ), segments);
    }

    @Test
    void consumesFormattingCodesAndLeavesMalformedCodesVisible() {
        List<MinecraftColorParser.Segment> segments = MinecraftColorParser.parse("§bA§lB §zC", 0xFFFFFF, 100);

        assertEquals(List.of(new MinecraftColorParser.Segment("AB §zC", 0x55FFFF)), segments);
    }

    @Test
    void truncatesByVisibleCharactersWithoutCountingColorCodes() {
        List<MinecraftColorParser.Segment> segments = MinecraftColorParser.parse("§cABCDE", 0xFFFFFF, 3);

        assertEquals(List.of(new MinecraftColorParser.Segment("ABC…", 0xFF5555)), segments);
    }

    @Test
    void parsesAmpersandRgbWithoutTreatingLegacyAmpersandAsColor() {
        List<MinecraftColorParser.Segment> segments = MinecraftColorParser.parse(
                "&#80b0d0Value &aLiteral",
                0xFFFFFF,
                100
        );

        assertEquals(List.of(new MinecraftColorParser.Segment("Value &aLiteral", 0x80B0D0)), segments);
    }
}
