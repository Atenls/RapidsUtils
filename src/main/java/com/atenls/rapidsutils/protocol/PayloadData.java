package com.atenls.rapidsutils.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public sealed interface PayloadData permits PayloadData.ObjectValue, PayloadData.ArrayValue, PayloadData.ScalarValue {
    record ObjectValue(Map<String, PayloadData> values) implements PayloadData {
        public ObjectValue {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }
    }

    record ArrayValue(List<PayloadData> values) implements PayloadData {
        public ArrayValue {
            values = List.copyOf(values);
        }
    }

    record ScalarValue(ScalarKind kind, String value) implements PayloadData {
        public ScalarValue {
            if (kind == ScalarKind.NULL) {
                value = "null";
            }
        }
    }

    enum ScalarKind {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
}
