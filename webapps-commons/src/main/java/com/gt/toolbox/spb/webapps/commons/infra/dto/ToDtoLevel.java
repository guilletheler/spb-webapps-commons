package com.gt.toolbox.spb.webapps.commons.infra.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ToDtoLevel {
    public EntityDetailLevel detailLevel() default EntityDetailLevel.COMPLETE;

}
