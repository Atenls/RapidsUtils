package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.text.MinecraftColorParser;

import java.util.List;

public record HudLine(int depth, List<MinecraftColorParser.Segment> spans) {
    public HudLine {
        spans = List.copyOf(spans);
    }
}
