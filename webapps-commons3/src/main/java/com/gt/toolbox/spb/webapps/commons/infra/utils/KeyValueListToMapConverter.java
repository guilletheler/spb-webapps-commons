package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gt.toolbox.spb.webapps.commons.infra.dto.KeyValueDto;

public class KeyValueListToMapConverter implements PropertyValueConverter {

    public KeyValueListToMapConverter() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convertValue(Object target, Object value) {
        if (value == null) {
            if (target != null) {
                ((Map<Object, Object>) target).clear();
            }
            target = null;
        } else {

            if (target == null) {
                target = newDefaultValue();
            } else {
                ((Map<Object, Object>) target).clear();
            }

            for (KeyValueDto<Object, Object> kv : (List<KeyValueDto<Object, Object>>) value) {
                ((Map<Object, Object>) target).put(kv.getKey(), kv.getValue());
            }
        }

        return target;
    }

    @Override
    public Object newDefaultValue() {
        return new HashMap<Object, Object>();
    }

}
