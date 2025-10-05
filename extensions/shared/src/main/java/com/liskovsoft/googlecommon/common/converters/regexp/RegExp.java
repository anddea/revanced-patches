package com.liskovsoft.googlecommon.common.converters.regexp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface RegExp {
    /**
     * Path for the desired json element
     */
    String[] value();
}
