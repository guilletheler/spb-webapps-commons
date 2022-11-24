package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
public class PropertyValueSetter {

    private String targetFieldName;

    private Field sourceField;
    private Method sourceMethod;

    private Field targetField;
    private Method targetMethod;
    private Method targetGetterMethod;

    private PropertyValueConverter converter;

    // private boolean replaceValue = true;

    public PropertyValueSetter(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public void setValue(Object target, Object source) {

        Object value = null;

        try {
            value = getSourceValue(source);

            if (converter != null) {

                Object targetValue = getTargetValue(target);

                if (targetValue == null) {
                    targetValue = this.converter.newDefaultValue();
                }
                if (targetValue != null) {
                    setTargetValue(target, targetValue);
                    converter.convertValue(targetValue, value);
                } else {
                    setTargetValue(target, value);
                }

            } else {
                // Logger.getLogger(getClass().getName()).log(Level.INFO,
                // "Seteando valor sin converter " + this.targetFieldName + " " +
                // getTargetValueClass().getName());
                setTargetValue(target, value);
            }
        } catch (Exception e) {
            logSetvalueException(source, value, e);
        }
    }

    private void logSetvalueException(Object source, Object value, Exception e) {
        String msg = "Convirtiendo entity a dto, illegal argument, seteando " + targetFieldName;

        if (targetField != null) {
            msg += "("
                    + targetField.getType();
        }
        msg += ") desde ";

        if (source != null) {
            msg += source.getClass().getName();
        }

        if (sourceField != null) {
            msg += "." + sourceField.getName() + ": " + sourceField.getType();
        }
        if (sourceMethod != null) {
            msg += "." + sourceMethod.getName() + "(): " + sourceMethod.getReturnType();
        }
        if (converter != null) {
            msg += " -> " + (value == null ? "null" : value.getClass().getName());
        }

        msg += "\n" + e.getLocalizedMessage();

        msg += "\n" + Utils.filterStackTrace(e, "com.gt").stream()
                .map(ste -> ste.getClassName() + "." + ste.getMethodName() + ":" + ste.getLineNumber())
                .collect(Collectors.joining("\n"));

        Logger.getLogger(getClass().getName()).log(Level.WARNING,
                msg);
    }

    private Object getTargetValue(Object target) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (targetGetterMethod == null) {
            if (this.targetField != null) {
                try {
                    return this.targetField.get(target);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    targetGetterMethod = target.getClass().getMethod("get" + StringUtils.capitalize(targetFieldName));
                }
            } else {
                targetGetterMethod = target.getClass().getMethod("get" + StringUtils.capitalize(targetFieldName));
            }
        }

        return targetGetterMethod.invoke(target);
    }

    private Object getSourceValue(Object source)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (this.sourceField != null) {
            return this.sourceField.get(source);
        }

        return sourceMethod.invoke(source);
    }

    private void setTargetValue(Object target, Object value)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (this.targetField != null) {
            this.targetField.set(target, value);
        }

        if (targetMethod != null) {
            this.targetMethod.invoke(target, value);
        } else {
            Logger.getLogger(getClass().getName()).log(Level.WARNING,
                    "No se asigna valor a " + targetFieldName + " porque no se pudo encontrar setter");
        }
    }

    /**
     * Clase del getter
     * 
     * @return
     */
    public Class<?> getSourceValueClass() {
        if (sourceField != null) {
            return sourceField.getType();
        }
        if (sourceMethod != null) {
            return sourceMethod.getReturnType();
        }

        return null;
    }

    /**
     * Clase del setter
     * 
     * @return
     */
    public Class<?> getTargetValueClass() {
        if (targetField != null) {
            return targetField.getType();
        }
        if (targetMethod != null) {
            if (targetMethod.getParameterTypes().length > 0) {
                return targetMethod.getParameterTypes()[0];
            } else {
                Logger.getLogger(getClass().getName()).log(Level.WARNING,
                        "El método target " + targetMethod.getName() + " no tiene parametros");
            }
        }

        return null;
    }
}