package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field or parameter for injection of an instance by the Emc4j testing framework.
 * <p>
 * This annotation can be applied in a JUnit test to fields or parameters to indicate that an instance should be automatically injected
 * at runtime. The optional {@code contextId} and {@code qualifier} elements can be used to specify the source context and qualifier
 * for the instance to inject.<br>
 * If contextId is not specified, the boot context will be used. If qualifier is not specified, the primary bean will be used if available, otherwise an unqualified bean will be used.<br>
 * </p>
 * <br>
 * <pre>
 * &commat;EmcInject
 * private MyService myService;
 * </pre>
 * <pre>
 * &commat;EmcInject(contextId = "00000000-0000-0000-0000-000000000001", qualifier = "specialService")
 * private MyService myService;
 * </pre>
 *
 * <pre>
 * &commat;Test
 * public my_test_method(@EmcInject(qualifier = "specialService") MyService service) {
 *     ...
 * }
 * </pre>
 *
 * @see com.treilhes.emc4j.test.Emc4jExtension
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface EmcInject {
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

    /**
     * Whether to create the definition in the context. If set to true, the
     * framework will attempt to create definition of the required type and inject
     * it. The default is false, which means that if the instance is not found in
     * the context, null will be injected.
     */
    boolean create() default false;
}