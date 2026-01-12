package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Emc4jExtensionContext {

    String[] properties() default {};
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
    /**
     * Exported classes from the extension
     * @return array of classes
     * @see OpenExtension#exportedContextClasses()
     */
    Class<?>[] exportedClasses() default {};

    Emc4jNestedExtensionContext[] extensions() default {};
}
