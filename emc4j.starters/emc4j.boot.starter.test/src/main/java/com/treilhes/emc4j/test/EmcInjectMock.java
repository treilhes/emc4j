package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field or parameter for injection of a mock instance by the Emc4j testing framework.
 * <p>
 * This annotation can be applied in a JUnit test to fields or parameters to indicate that a mock instance should be automatically injected
 * at runtime. The optional {@code contextId} and {@code qualifier} elements can be used to specify the context and qualifier
 * for the mock instance to inject.<br>
 * If contextId is not specified, the boot context will be targeted.
 * Injecting the same mock twice will override the first definition.
 * </p>
 * You can also annotate the same field or parameter with @Primary or @Scope
 * <br>
 * <pre>
 * &commat;EmcInjectMock
 * private MyService myService;
 * </pre>
 * <pre>
 * &commat;EmcInjectMock(contextId = "", qualifier = "specialService")
 * private MyService myService;
 * </pre>
 *
 * <pre>
 * &commat;Test
 * public my_test_method(@EmcInjectMock(qualifier = "specialService") MyService service) {
 *     ...
 * }
 * </pre>
 *
 * @see com.treilhes.emc4j.test.Emc4jExtension
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface EmcInjectMock {
    /**
     * The contextId (UUID format ex: 00000000-0000-0000-0000-000000000001) to
     * identify the context into which the mock should be injected. If not
     * specified, the boot context will be used.
     */
    String contextId() default "";

    /**
     * The qualifier to use to identify the name of the bean used to insert the bean definition
     * into the context. If not specified, the primary bean will be used if
     * available, otherwise an unqualified bean will be used.
     */
    String qualifier() default "";
}