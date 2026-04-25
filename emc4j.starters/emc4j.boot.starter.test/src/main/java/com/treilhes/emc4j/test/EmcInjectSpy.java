package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field or parameter for injection of a spy instance by the Emc4j testing framework.
 * <p>
 * This annotation can be applied in a JUnit test to fields or parameters to indicate that a spy instance should be automatically injected
 * at runtime. The optional {@code contextId} and {@code qualifier} elements can be used to specify the context and qualifier
 * for the spy instance to inject.<br>
 * If contextId is not specified, the boot context will be targeted.
 * A bean must already be defined in the context to be spied.
 * </p>
 * <br>
 * <pre>
 * &commat;EmcInjectSpy
 * private MyService myService;
 * </pre>
 * <pre>
 * &commat;EmcInjectSpy(contextId = "", qualifier = "specialService")
 * private MyService myService;
 * </pre>
 *
 * <pre>
 * &commat;Test
 * public my_test_method(@EmcInjectSpy(qualifier = "specialService") MyService service) {
 *     ...
 * }
 * </pre>
 *
 * @see com.treilhes.emc4j.test.Emc4jExtension
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface EmcInjectSpy {
    /**
     * The contextId (UUID format ex: 00000000-0000-0000-0000-000000000001) to use as source
     * for injection. If not specified, the boot context will be used.
     */
    String contextId() default "";

    /**
     * The qualifier to use for injection. If not specified, the primary bean will
     * be used if available, otherwise an unqualified bean will be used.
     */
    String qualifier() default "";
}