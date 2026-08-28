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
                 "data":{"wave":3,"members":["A",true,null]}}
                """);

        assertTrue(parsed.isPresent());
        DataEnvelope envelope = parsed.orElseThrow();
        assertEquals("dungeon", envelope.topic());
        assertEquals(42, envelope.sequence());
        PayloadData.ObjectValue root = assertInstanceOf(PayloadData.ObjectValue.class, envelope.data());
        assertEquals("3", assertInstanceOf(PayloadData.ScalarValue.class, root.values().get("wave")).value());
        PayloadData.ArrayValue members = assertInstanceOf(PayloadData.ArrayValue.class, root.values().get("members"));
        assertEquals(3, members.values().size());
    }

    @Test
    void rejectsMalformedUnsupportedAndPartialPayloads() {
        assertTrue(DataEnvelopeParser.parse("not json").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":2,\"topic\":\"boss\",\"sequence\":1,\"full\":true,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1,\"full\":false,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1.5,\"full\":true,\"data\":{}}").isEmpty());
        assertTrue(DataEnvelopeParser.parse("{\"version\":1,\"topic\":\"boss\",\"sequence\":1,\"full\":true}").isEmpty());
    }
}
