package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import com.treilhes.emc4j.boot.api.context.annotation.Primary;

/**
 * Ensure EmInject works as expected. This is a very basic test, just to check
 * that the annotation is properly processed and that the injection works. More
 * complex tests should be added to test the various features of EmInject. <br>
 * <br>
 * EmInject can be used on method parameters and on attributes Without any
 * arguments, it should inject beans from the root context With the contextId
 * argument, it should inject beans from the specified context
 */
@Emc4jTest(classes = { EmcInjectTest.BootConfig.class }, context = @Emc4jCoreContext(classes = {
        EmcInjectTest.RootConfig.class }, applications = {
                @Emc4jApplicationContext(uuid = EmcInjectTest.APP_ID, classes = { EmcInjectTest.AppConfig.class }) }))
class EmcInjectTest {

    public static final String BOOT_ID = "00000000-0000-0000-0000-000000000000";
    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000001";
    public static final String APP_ID = "00000000-0000-0000-0000-000000000002";

    public static final String BOOT_UNQUALIFIED = "BOOT_UNQUALIFIED";
    public static final String BOOT_QUALIFIED = "BOOT_QUALIFIED";
    public static final String ROOT_UNQUALIFIED = "ROOT_UNQUALIFIED";
    public static final String ROOT_QUALIFIED = "ROOT_QUALIFIED";
    public static final String APP_UNQUALIFIED = "APP_UNQUALIFIED";
    public static final String APP_QUALIFIED = "APP_QUALIFIED";

    static class BootConfig {
        @Bean
        @Primary
        String bootUnqualifiedBean() {
            return BOOT_UNQUALIFIED;
        }

        @Bean(BOOT_QUALIFIED)
        String bootQualifiedBean() {
            return BOOT_QUALIFIED;
        }
    }

    static class RootConfig {
        @Bean
        @Primary
        String rootUnqualifiedBean() {
            return ROOT_UNQUALIFIED;
        }

        @Bean(ROOT_QUALIFIED)
        String rootQualifiedBean() {
            return ROOT_QUALIFIED;
        }
    }

    static class AppConfig {
        @Bean
        @Primary
        String appUnqualifiedBean() {
            return APP_UNQUALIFIED;
        }

        @Bean(APP_QUALIFIED)
        String appQualifiedBean() {
            return APP_QUALIFIED;
        }
    }

    @EmcInject
    String boot;

    @EmcInject(contextId = BOOT_ID)
    String boot2;

    @EmcInject(qualifier = BOOT_QUALIFIED)
    String bootQualified;

    @EmcInject(contextId = BOOT_ID, qualifier = BOOT_QUALIFIED)
    String bootQualified2;

    @EmcInject(contextId = ROOT_ID)
    String root;

    @EmcInject(contextId = ROOT_ID, qualifier = ROOT_QUALIFIED)
    String rootQualified;

    @EmcInject(contextId = APP_ID)
    String app;

    @EmcInject(contextId = APP_ID, qualifier = APP_QUALIFIED)
    String appQualified;

    @Test
    void must_inject_bean_from_boot_context_without_annotation_parameter() {
        assertEquals(BOOT_UNQUALIFIED, boot);
    }

    @Test
    void must_inject_bean_from_boot_context_with_contextId_parameter() {
        assertEquals(BOOT_UNQUALIFIED, boot2);
    }

    @Test
    void must_inject_bean_from_boot_context_with_qualifier_parameter() {
        assertEquals(BOOT_QUALIFIED, bootQualified);
    }

    @Test
    void must_inject_bean_from_boot_context_with_contextId_and_qualifier_parameter() {
        assertEquals(BOOT_QUALIFIED, bootQualified2);
    }

    @Test
    void must_inject_bean_from_boot_context_without_annotation_parameter_in_method_params(@EmcInject String boot3) {
        assertEquals(BOOT_UNQUALIFIED, boot3);
    }

    @Test
    void must_inject_bean_from_boot_context_with_contextId_parameter_in_method_params(
            @EmcInject(contextId = BOOT_ID) String boot3) {
        assertEquals(BOOT_UNQUALIFIED, boot3);
    }

    @Test
    void must_inject_bean_from_boot_context_with_qualifier_parameter_in_method_params(
            @EmcInject(qualifier = BOOT_QUALIFIED) String bootQualified3) {
        assertEquals(BOOT_QUALIFIED, bootQualified3);
    }

    @Test
    void must_inject_bean_from_boot_context_with_contextId_and_qualifier_parameter_in_method_params(
            @EmcInject(contextId = BOOT_ID, qualifier = BOOT_QUALIFIED) String bootQualified3) {
        assertEquals(BOOT_QUALIFIED, bootQualified3);
    }

    @Test
    void must_inject_bean_from_root_context_without_annotation_parameter() {
        assertEquals(ROOT_UNQUALIFIED, root);
    }

    @Test
    void must_inject_bean_from_root_context_with_contextId_parameter() {
        assertEquals(ROOT_QUALIFIED, rootQualified);
    }

    @Test
    void must_inject_bean_from_root_context_without_annotation_parameter_in_method_params(
            @EmcInject(contextId = ROOT_ID) String root2) {
        assertEquals(ROOT_UNQUALIFIED, root2);
    }

    @Test
    void must_inject_bean_from_root_context_with_contextId_parameter_in_method_params(
            @EmcInject(contextId = ROOT_ID, qualifier = ROOT_QUALIFIED) String rootQualified2) {
        assertEquals(ROOT_QUALIFIED, rootQualified2);
    }

    @Test
    void must_inject_bean_from_app_context_without_annotation_parameter() {
        assertEquals(APP_UNQUALIFIED, app);
    }

    @Test
    void must_inject_bean_from_app_context_with_contextId_parameter() {
        assertEquals(APP_QUALIFIED, appQualified);
    }

    @Test
    void must_inject_bean_from_app_context_without_annotation_parameter_in_method_params(
            @EmcInject(contextId = APP_ID) String app2) {
        assertEquals(APP_UNQUALIFIED, app2);
    }

    @Test
    void must_inject_bean_from_app_context_with_contextId_parameter_in_method_params(
            @EmcInject(contextId = APP_ID, qualifier = APP_QUALIFIED) String appQualified2) {
        assertEquals(APP_QUALIFIED, appQualified2);
    }
}
