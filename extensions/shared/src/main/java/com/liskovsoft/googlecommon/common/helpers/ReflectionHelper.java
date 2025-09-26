package com.liskovsoft.googlecommon.common.helpers;

import com.liskovsoft.googlecommon.common.converters.FieldNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.revanced.extension.shared.utils.Logger;

public class ReflectionHelper {
    public static Class<?> getGenericParamType(Field field) {
        Type[] params = getGenericParams(field);

        if (params != null && params.length == 1) {
            return (Class<?>) params[0];
        }

        return null;
    }

    public static Type[] getGenericParams(Field field) {
        Type genericFieldType = field.getGenericType();

        if (genericFieldType instanceof ParameterizedType aType) {
            return aType.getActualTypeArguments();
        }

        return null;
    }

    public static boolean isAssignableFrom(Field field, Class<?> targetType) {
        return field.getType().isAssignableFrom(targetType);
    }

    public static List<Field> getAllFields(Class<?> type) {
        Field[] fields = type.getDeclaredFields();

        List<Field> result = new ArrayList<>(Arrays.asList(fields));

        while (type.getSuperclass() != null) { // null if superclass is object
            type = type.getSuperclass();
            result.addAll(Arrays.asList(type.getDeclaredFields()));
            // ??? Speedup json parsing by putting on top important fields.
            //Collections.sort(result, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        }

        return result;
    }

    public static void setField(Field field, Object obj, Object val) {
        try {
            field.set(obj, val);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            Logger.printException(() -> "setField failed");
        }
    }

    public static boolean isNullable(Field field) {
        Annotation[] annotations = field.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof FieldNullable) {
                return true;
            }
        }

        return false;
    }
}
