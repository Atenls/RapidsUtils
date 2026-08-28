package com.atenls.rapidsutils.protocol;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataEnvelopeParserTest {
    @Test
    void parsesArbitraryNestedDataIntoImmutableValues() {
        Optional<DataEnvelope> parsed = DataEnvelopeParser.parse("""
                {"version":1,"topic":"dungeon","sequence":42,"full":true,
                 "duration":60.5,"index":2,
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
        PayloadData.ObjectValue root = assertInstanceOf(PayloadData.ObjectValue.class, envelope.data());
        assertEquals("3", assertInstanceOf(PayloadData.ScalarValue.class, root.values().get("wave")).value());
        PayloadData.ArrayValue members = assertInstanceOf(PayloadData.ArrayValue.class, root.values().get("members"));
        assertEquals(3, members.values().size());
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

        DataEnvelope futureValues = DataEnvelopeParser.parse("""
                {"version":1,"topic":"future","sequence":1,"full":true,
                 "duration":{"mode":"manual"},"index":[1,2],"data":{}}
                """).orElseThrow();
        assertInstanceOf(PayloadData.ObjectValue.class, futureValues.duration());
        assertInstanceOf(PayloadData.ArrayValue.class, futureValues.index());
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
