package com.atenls.rapidsutils.protocol;

import com.atenls.rapidsutils.state.TopicSnapshotStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEnvelopeParserTest {
    private static final String NUMERIC_PAYLOAD = """
            {"version":1,"topic":"probe","sequence":1,"full":true,
             "duration":1,"index":1,"x":1,"y":1,"opacity":1,"fadeIn":1,"fadeOut":1,
             "data":{"display":"unchanged"}}
            """;

    @Test
    void rejectsExtremeNumbersInEveryControlFieldWithoutThrowing() {
        for (String field : java.util.List.of("version", "sequence", "duration", "index", "x", "y",
                "opacity", "fadeIn", "fadeOut")) {
            for (String raw : java.util.List.of("1.1e-2147483647", "1e1000000", "1e-1000000",
                    "9".repeat(129), "1e+" + "0".repeat(254) + "1")) {
                String json = NUMERIC_PAYLOAD.replace("\"" + field + "\":1", "\"" + field + "\":" + raw);
                assertTrue(DataEnvelopeParser.parse(json).isEmpty(), field + "=" + raw);
            }
        }
    }

    @Test
    void invalidControlNumberLeavesSnapshotAndSequenceUntouched() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        DataEnvelopeParser.parse(NUMERIC_PAYLOAD).ifPresent(store::apply);
        var before = store.snapshot();
        String next = NUMERIC_PAYLOAD.replace("\"sequence\":1", "\"sequence\":2");

        DataEnvelopeParser.parse(next.replace("\"duration\":1", "\"duration\":1.1e-2147483647"))
                .ifPresent(store::apply);
        assertSame(before, store.snapshot());
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(DataEnvelopeParser.parse(next).orElseThrow()));
    }

    @Test
    void acceptsBoundedScientificNumbersAndPreservesArbitraryDataNumbers() {
        String json = NUMERIC_PAYLOAD
                .replace("\"sequence\":1", "\"sequence\":9223372036854775807")
                .replace("\"duration\":1", "\"duration\":1e128")
                .replace("\"index\":1", "\"index\":" + "9".repeat(128) + "e128")
                .replace("\"x\":1", "\"x\":1e-128")
                .replace("\"display\":\"unchanged\"", "\"display\":1.1e-2147483647");
        DataEnvelope envelope = DataEnvelopeParser.parse(json).orElseThrow();
        assertEquals(Long.MAX_VALUE, envelope.sequence());
        assertTrue(Double.isFinite(envelope.durationTicks().orElseThrow().doubleValue()));
        assertTrue(Double.isFinite(envelope.sortIndex().orElseThrow().doubleValue()));
        assertTrue(envelope.resolvedScreenX(320).orElseThrow().doubleValue() > 0.0D);
        var data = assertInstanceOf(PayloadData.ObjectValue.class, envelope.data());
        assertEquals("1.1e-2147483647", assertInstanceOf(PayloadData.ScalarValue.class, data.values().get("display")).value());
    }

    @Test
    void parsesArbitraryNestedDataIntoImmutableValues() {
        Optional<DataEnvelope> parsed = DataEnvelopeParser.parse("""
                {"version":1,"topic":"dungeon","sequence":42,"full":true,
                 "duration":60.5,"index":2,"x":160.5,"y":90,"opacity":0.75,
                 "fadeIn":8,"fadeOut":20,
                 "data":{"wave":3,"members":["A",true,null]}}
                """);

        assertTrue(parsed.isPresent());
        DataEnvelope envelope = parsed.orElseThrow();
        assertEquals("dungeon", envelope.topic());
        assertEquals(42, envelope.sequence());
        assertEquals("60.5", assertInstanceOf(PayloadData.ScalarValue.class, envelope.duration()).value());
        assertEquals("2", assertInstanceOf(PayloadData.ScalarValue.class, envelope.index()).value());
        assertEquals("60.5", envelope.durationTicks().orElseThrow().toPlainString());
        assertEquals("2", envelope.sortIndex().orElseThrow().toPlainString());
        assertEquals("160.5", envelope.screenX().orElseThrow().toPlainString());
        assertEquals("90", envelope.screenY().orElseThrow().toPlainString());
        assertEquals("0.75", envelope.panelOpacity().orElseThrow().toPlainString());
        assertEquals("8", envelope.fadeInTicks().orElseThrow().toPlainString());
        assertEquals("20", envelope.fadeOutTicks().orElseThrow().toPlainString());
        PayloadData.ObjectValue root = assertInstanceOf(PayloadData.ObjectValue.class, envelope.data());
        assertEquals("3", assertInstanceOf(PayloadData.ScalarValue.class, root.values().get("wave")).value());
        PayloadData.ArrayValue members = assertInstanceOf(PayloadData.ArrayValue.class, root.values().get("members"));
        assertEquals(3, members.values().size());
    }

    @Test
    void resolvesFractionalCoordinatesAsScreenPercentages() {
        DataEnvelope fractional = DataEnvelopeParser.parse("""
                {"version":1,"topic":"fractional","sequence":1,"full":true,
                 "duration":null,"index":null,"x":0.5,"y":0.25,"data":{}}
                """).orElseThrow();
        assertEquals("160.0", fractional.resolvedScreenX(320).orElseThrow().toPlainString());
        assertEquals("50.00", fractional.resolvedScreenY(200).orElseThrow().toPlainString());

        DataEnvelope pixels = DataEnvelopeParser.parse("""
                {"version":1,"topic":"pixels","sequence":1,"full":true,
                 "duration":null,"index":null,"x":0,"y":1.0,"data":{}}
                """).orElseThrow();
        assertEquals("0", pixels.resolvedScreenX(320).orElseThrow().toPlainString());
        assertEquals("1.0", pixels.resolvedScreenY(200).orElseThrow().toPlainString());
    }

    @Test
    void preservesNullAndNonNumericControlValues() {
        DataEnvelope nullValues = DataEnvelopeParser.parse("""
                {"version":1,"topic":"legacy","sequence":1,"full":true,
                 "duration":null,"index":null,"data":{}}
                """).orElseThrow();
        assertEquals(PayloadData.ScalarKind.NULL,
                assertInstanceOf(PayloadData.ScalarValue.class, nullValues.duration()).kind());
        assertEquals(PayloadData.ScalarKind.NULL,
                assertInstanceOf(PayloadData.ScalarValue.class, nullValues.index()).kind());
        assertTrue(nullValues.durationTicks().isEmpty());
        assertTrue(nullValues.sortIndex().isEmpty());
        assertTrue(nullValues.screenX().isEmpty());
        assertTrue(nullValues.screenY().isEmpty());
        assertTrue(nullValues.panelOpacity().isEmpty());
        assertTrue(nullValues.fadeInTicks().isEmpty());
        assertTrue(nullValues.fadeOutTicks().isEmpty());

        DataEnvelope futureValues = DataEnvelopeParser.parse("""
                {"version":1,"topic":"future","sequence":1,"full":true,
                 "duration":{"mode":"manual"},"index":[1,2],
                 "x":"center","y":{},"opacity":true,"fadeIn":-1,"fadeOut":"slow","data":{}}
                """).orElseThrow();
        assertInstanceOf(PayloadData.ObjectValue.class, futureValues.duration());
        assertInstanceOf(PayloadData.ArrayValue.class, futureValues.index());
        assertTrue(futureValues.screenX().isEmpty());
        assertTrue(futureValues.screenY().isEmpty());
        assertTrue(futureValues.panelOpacity().isEmpty());
        assertTrue(futureValues.fadeInTicks().isEmpty());
        assertTrue(futureValues.fadeOutTicks().isEmpty());
    }

    @Test
    void rejectsMalformedUnsupportedAndPartialPayloads() {
        assertTrue(DataEnvelopeParser.parse("not json").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":2,\"topic\":\"boss\",\"sequence\":1,\"full\":true,\"duration\":null,\"index\":null,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1,\"full\":false,\"duration\":null,\"index\":null,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1.5,\"full\":true,\"duration\":null,\"index\":null,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1,\"full\":true,\"duration\":null,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1,\"full\":true,\"index\":null,\"data\":{}}").isEmpty());
    }
}
