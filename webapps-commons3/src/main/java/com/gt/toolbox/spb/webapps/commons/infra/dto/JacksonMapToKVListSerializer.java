package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.extern.java.Log;

@Log
public class JacksonMapToKVListSerializer<K, V> extends StdSerializer<Map<K, V>> {

    public JacksonMapToKVListSerializer() {
        this(null);
    }

    public JacksonMapToKVListSerializer(Class<Map<K, V>> t) {
        super(t);
    }

    @Override
    public void serialize(Map<K, V> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        if (gen.getOutputContext().getCurrentName() == null) {
            log.warning("El nombre es nulo");
        }

        gen.writeStartArray();
        if (value != null && !value.isEmpty()) {
            var toSerialize = CollectionsDtoUtils.of(value);

            gen.writeObjectField(gen.getOutputContext().getCurrentName(), toSerialize);
        }
        gen.writeEndArray();
    }
}
