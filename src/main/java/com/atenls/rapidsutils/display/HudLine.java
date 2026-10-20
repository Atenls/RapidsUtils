package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.text.MinecraftColorParser;

import java.util.ArrayList;
import java.util.List;

public record HudLine(int depth, List<MinecraftColorParser.Segment> spans) {
    public HudLine {
        spans = List.copyOf(spans);
    }

    public HudLine withRepeatCount(long count) {
        if (count <= 1L) {
            return this;
        }
        ArrayList<MinecraftColorParser.Segment> counted = new ArrayList<>(spans);
        counted.add(new MinecraftColorParser.Segment(" (x" + count + ")", 0xAAAAAA));
        return new HudLine(depth, counted);
    }
}
