package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertyValueSetterConfig {
    public EntityDetailLevel detailLevel() default EntityDetailLevel.COMPLETE;

    public String sourceField() default "";

    public String sourceMethod() default "";

    public Class<?> converterClass() default Object.class;
    
    public EntityDetailLevel childDetailLevel() default EntityDetailLevel.SELECT;
    
    public Class<?> collectionClass() default Object.class;
}