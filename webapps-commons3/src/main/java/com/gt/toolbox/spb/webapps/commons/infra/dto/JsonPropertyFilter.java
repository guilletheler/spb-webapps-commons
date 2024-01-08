package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.util.List;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import lombok.extern.java.Log;

@Log
public class JsonPropertyFilter extends JacksonAnnotationIntrospector {

    Class<?> destClass;
    List<String> excludeProperies = null;
    List<String> includeProperies = null;

    public JsonPropertyFilter(Class<?> destClass, List<String> excludeProperies) {
        this(destClass, excludeProperies, null);
    }

    /**
     * @param dtoMainClass
     */
    public JsonPropertyFilter(Class<?> destClass, List<String> excludeProperies,
            List<String> includeProperies) {
        this.destClass = destClass;

        if (excludeProperies != null && !excludeProperies.isEmpty()) {
            this.excludeProperies =
                    excludeProperies.stream().map(s -> destClass.getName() + "#" + s).toList();
        }

        if (includeProperies != null && !includeProperies.isEmpty()) {
            this.includeProperies =
                    includeProperies.stream().map(s -> destClass.getName() + "#" + s).toList();
        }
    }

    @Override
    public boolean hasIgnoreMarker(final AnnotatedMember m) {

        log.info(m.getDeclaringClass().getName());
        log.info(m.getFullName() + " se incluye?");



        var ret = false;
        if (super.hasIgnoreMarker(m)) {
            ret = true;
        } else {
            if (excludeProperies != null) {
                ret = excludeProperies.contains(m.getName());
            }

            if (!ret && includeProperies != null) {
                log.info(includeProperies.toString() + " incluye? " + m.getFullName());
                ret = !includeProperies.contains(m.getFullName());
            }

        }

        log.info(m.getFullName() + " se incluye: " + !ret);
        return ret;
    }

}
