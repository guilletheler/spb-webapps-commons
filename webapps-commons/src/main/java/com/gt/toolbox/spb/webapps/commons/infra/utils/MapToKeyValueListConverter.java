package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gt.toolbox.spb.webapps.commons.infra.dto.KeyValueDto;

public class MapToKeyValueListConverter implements PropertyValueConverter {

    public MapToKeyValueListConverter() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convertValue(Object target, Object value) {
        if (value == null) {
            if (target != null) {
                ((List<KeyValueDto<String, Object>>) target).clear();
            }
            target = null;
        } else {

            if (target == null) {
                target = newDefaultValue();
            } else {
                ((List<KeyValueDto<String, Object>>) target).clear();
            }

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                ((List<KeyValueDto<String, Object>>) target)
                        .add(new KeyValueDto<>(entry.getKey().toString(), entry.getValue()));
            }
        }

        return target;
    }

    @Override
    public Object newDefaultValue() {
        return new ArrayList<KeyValueDto<String, Object>>();
    }

}
