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

public class JacksonKVListToMapDeserializer<K, V> extends StdDeserializer<Map<K, V>> {

    protected JacksonKVListToMapDeserializer() {
        this(null);
    }

    protected JacksonKVListToMapDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<K, V> deserialize(JsonParser parser, DeserializationContext ctxt)
            throws IOException, JacksonException {


        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        var jsonString = node.toString();

        ObjectMapper mapper = new ObjectMapper();

        List<KeyValueDto<K, V>> lst = new ArrayList<>();

        lst = mapper.readValue(jsonString, lst.getClass());

        Map<K, V> ret = new HashMap<K, V>();

        CollectionsDtoUtils.sinchronize(ret, lst);

        return ret;

    }
}

