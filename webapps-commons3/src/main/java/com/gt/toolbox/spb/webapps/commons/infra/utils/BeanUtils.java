package com.gt.toolbox.spb.webapps.commons.infra.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class BeanUtils {

    public static <D, S> List<D> buildAndFill(List<S> sources, Class<D> destClass, EntityDetailLevel level) {

        List<D> retList = new ArrayList<>();
        if (!sources.isEmpty()) {

            try {
                var fieldSetters = buildPropertiesSetter(sources.get(0).getClass(), destClass, level);

                for (var source : sources) {
                    @SuppressWarnings("unchecked")
                    D ret = (D) destClass.getDeclaredConstructors()[0].newInstance();
                    fieldSetters.forEach(fs -> fs.setValue(ret, source));
                    retList.add(ret);
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException
                    | SecurityException e) {
                Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING, "Error clonando objeto");
            }
        }

        return retList;
    }

    public static <D, S> D buildAndFill(S source, Class<D> destClass, EntityDetailLevel level) {

        try {
            @SuppressWarnings("unchecked")
            D ret = (D) destClass.getDeclaredConstructors()[0].newInstance();
            var fieldSetters = buildPropertiesSetter(source.getClass(), destClass, level);
            fieldSetters.forEach(fs -> {
                fs.setValue(ret, source);
            });
            return ret;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException e) {
            Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING, "Error clonando objeto");
        }

        return null;
    }

    public static <S, D> void fillProperties(S source, D target, EntityDetailLevel level) {
        List<PropertyValueSetter> fieldSetter = buildPropertiesSetter(source.getClass(), target.getClass(), level);

        for (PropertyValueSetter fieldConverter : fieldSetter) {
            fieldConverter.setValue(target, source);
        }
    }

    private static List<PropertyValueSetter> buildPropertiesSetter(Class<?> sourceClass, Class<?> targetClass,
            EntityDetailLevel level) {

        List<PropertyValueSetter> fieldValuesAssigner = new ArrayList<>();

        for (Field f : targetClass.getDeclaredFields()) {

            PropertyValueSetterConfig config = f.getAnnotation(PropertyValueSetterConfig.class);

            EntityDetailLevel fieldLevel = getEntityDetailLevel(config);

            if (fieldLevel.ordinal() >= level.ordinal()) {

                PropertyValueSetter propertyValueSetter = new PropertyValueSetter(f.getName());
                // es por cuestiones de logueo
                propertyValueSetter.setTargetFieldName(f.getName());

                try {
                    propertyValueSetter.setSourceField(getSourceField(config, sourceClass, f));
                } catch (NoSuchFieldException | SecurityException e) {
                    try {
                        propertyValueSetter.setSourceMethod(getSourceMethod(config, sourceClass, f));
                    } catch (NoSuchMethodException | SecurityException | NoSuchFieldException e1) {
                        Utils.logError(BeanUtils.class,
                                new Exception("No se puede setear origen de property setter", e1));
                        continue;
                    }
                }

                try {
                    propertyValueSetter.setTargetField(getTargetField(config, targetClass, f));
                } catch (NoSuchFieldException | SecurityException e) {
                    try {
                        propertyValueSetter.setTargetMethod(getTargetMethod(config, targetClass, f));
                    } catch (NoSuchMethodException | SecurityException | NoSuchFieldException e1) {
                        Utils.logError(BeanUtils.class,
                                new Exception("No se puede setear destino de property setter", e1));
                        continue;
                    }
                }

                setConverter(config, propertyValueSetter);

                fieldValuesAssigner.add(propertyValueSetter);

            }

        }

        return fieldValuesAssigner;
    }

    private static void setConverter(PropertyValueSetterConfig config, PropertyValueSetter propertyValueSetter) {

        if (config != null && config.converterClass() != null && config.converterClass() != Object.class) {
            try {
                propertyValueSetter.setConverter(
                        (PropertyValueConverter) config.converterClass().getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING,
                        "No se puede instanciar converter " + config.converterClass());
            } catch (java.lang.ClassCastException e1) {
                Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING,
                        "No se puede convertir a PropertyValueConverter la clase " + config.converterClass());
            }
        } else {
            if (propertyValueSetter.getSourceValueClass() == null
                    || propertyValueSetter.getTargetValueClass() == null) {
                Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING,
                        "Clases de origen o destino nulo");
            } else if (propertyValueSetter.getSourceValueClass() != propertyValueSetter.getTargetValueClass()) {
                if (Map.class.isAssignableFrom(propertyValueSetter.getSourceValueClass())
                        && List.class.isAssignableFrom(propertyValueSetter.getTargetValueClass())) {
                    // supongo trata de convertir map<String, Object> a List<KeyValueDto<String,
                    // Object>>
                    propertyValueSetter.setConverter(new MapToKeyValueListConverter());
                } else if (Map.class.isAssignableFrom(propertyValueSetter.getTargetValueClass())
                        && List.class.isAssignableFrom(propertyValueSetter.getSourceValueClass())) {
                    propertyValueSetter.setConverter(new KeyValueListToMapConverter());
                } else {
                    // supongo un EntityToDtoConverter con level LIST

                    EntityDetailLevel childLevel = EntityDetailLevel.SELECT;

                    if (config != null && config.childDetailLevel() != null) {
                        childLevel = config.childDetailLevel();
                    }

                    propertyValueSetter.setConverter(
                            new EntityToDtoConverter(propertyValueSetter.getTargetValueClass(), childLevel));

                    // Logger.getLogger(BeanUtils.class.getName()).log(Level.WARNING,
                    // "Supongo que es otro dto");
                }

            } else if (List.class.isAssignableFrom(propertyValueSetter.getSourceValueClass())) {
                if (config != null && config.collectionClass() != Object.class) {
                    EntityDetailLevel childLevel = EntityDetailLevel.SELECT;
                    if (config != null && config.childDetailLevel() != null) {
                        childLevel = config.childDetailLevel();
                    }
                    propertyValueSetter.setConverter(
                            new CollectionConverter(ArrayList.class, config.collectionClass(), childLevel));
                } else {
                    propertyValueSetter.setConverter(new CollectionConverter(ArrayList.class));
                }
            } else if (Set.class.isAssignableFrom(propertyValueSetter.getSourceValueClass())) {
                propertyValueSetter.setConverter(new CollectionConverter(HashSet.class));
            }
        }

    }

    private static EntityDetailLevel getEntityDetailLevel(PropertyValueSetterConfig config) {
        if (config == null || config.detailLevel() == null) {
            return EntityDetailLevel.COMPLETE;
        }

        return config.detailLevel();
    }

    private static Field getSourceField(PropertyValueSetterConfig propertyValueSetterConfig, Class<?> sourceClass,
            Field sourceField) throws NoSuchFieldException, SecurityException {
        Field ret = null;

        if (propertyValueSetterConfig != null && propertyValueSetterConfig.sourceField() != null
                && !propertyValueSetterConfig.sourceField().isEmpty()) {
            ret = sourceClass.getField(propertyValueSetterConfig.sourceField());
        } else {
            ret = sourceClass.getField(sourceField.getName());
        }

        return ret;
    }

    private static Method getSourceMethod(PropertyValueSetterConfig propertyValueSetterConfig, Class<?> sourceClass,
            Field targetField) throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        Method ret = null;

        if (propertyValueSetterConfig != null && propertyValueSetterConfig.sourceMethod() != null
                && !propertyValueSetterConfig.sourceMethod().isEmpty()) {
            ret = sourceClass.getMethod(propertyValueSetterConfig.sourceMethod());
        } else {
            ret = getGetterMethod(sourceClass, targetField);
        }

        return ret;
    }

    private static Field getTargetField(PropertyValueSetterConfig propertyValueSetterConfig, Class<?> targetClass,
            Field targetField) throws NoSuchFieldException, SecurityException {
        Field ret = null;

        targetClass.getField(targetField.getName());

        return ret;
    }

    private static Method getTargetMethod(PropertyValueSetterConfig propertyValueSetterConfig, Class<?> targetClass,
            Field targetField) throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        Method ret = null;
        ret = getSetterMethod(targetClass, targetField);

        return ret;
    }

    public static Method getGetterMethod(Class<?> clazz, String fieldName)
            throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        Field field = clazz.getDeclaredField(fieldName);

        String methodName = getGetterMethodName(field.getName(), field.getType());

        return clazz.getMethod(methodName);
    }

    public static Method getGetterMethod(Class<?> clazz, Field field) throws NoSuchMethodException, SecurityException {
        String methodName = getGetterMethodName(field.getName(), field.getType());

        return clazz.getMethod(methodName);
    }

    public static String getGetterMethodName(Field field) {
        return getGetterMethodName(field.getName(), field.getType());
    }

    public static String getGetterMethodName(String fieldName) {
        return getGetterMethodName(fieldName, false);
    }

    public static String getGetterMethodName(String fieldName, Class<?> fieldClazz) {
        return getGetterMethodName(fieldName, fieldClazz.getName().equals("boolean"));
    }

    public static String getGetterMethodName(String fieldName, boolean isBoolean) {
        String methodName = StringUtils.capitalize(fieldName);
        if (isBoolean) {
            methodName = "is" + methodName;
        } else {
            methodName = "get" + methodName;
        }
        return methodName;
    }

    public static Method getSetterMethod(Class<?> clazz, String fieldName)
            throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        String methodName = getSetterMethodName(fieldName);

        Field field = clazz.getDeclaredField(fieldName);

        return clazz.getMethod(methodName, field.getType());
    }

    public static Method getSetterMethod(Class<?> clazz, Field field)
            throws NoSuchMethodException, SecurityException, NoSuchFieldException {

        return getSetterMethod(clazz, field.getName());
    }

    public static String getSetterMethodName(String fieldName) {
        String methodName = "set" + StringUtils.capitalize(fieldName);
        return methodName;
    }
}
