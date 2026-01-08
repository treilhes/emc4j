package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Emc4jApplicationContext {

    /**
     * UUID of the extension
     * @return UUID string
     * @see Extension#getId()
     */
    String uuid() default "";
    /**
     * Context classes to load
     * @return array of classes
     * @see Extension#localContextClasses()
     */
    Class<?>[] classes() default {};

    Emc4jExtensionContext[] extensions() default {};
}
