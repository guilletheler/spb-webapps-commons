package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JacksonMapToKVListDeserializer<K, V> extends StdDeserializer<List<KeyValueDto<K, V>>> {

    protected JacksonMapToKVListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<KeyValueDto<K, V>> deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JacksonException {


        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        var jsonString = node.toString();

        ObjectMapper mapper = new ObjectMapper();

        Map<K, V> map = new HashMap<>();

        map = mapper.readValue(jsonString, map.getClass());

        return CollectionsDtoUtils.of(map);

    }

}
