package com.treilhes.emc4j.test.mapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.test.Emc4jApplicationContext;
import com.treilhes.emc4j.test.Emc4jCoreContext;
import com.treilhes.emc4j.test.Emc4jDefault;
import com.treilhes.emc4j.test.Emc4jExtensionContext;
import com.treilhes.emc4j.test.Emc4jNestedExtensionContext;
import com.treilhes.emc4j.test.Emc4jTest;

public class AnnotationMapperTest {

    @Test
    public void test_Emc4jTest_empty_annotation_mapping() {

        @Emc4jTest
        class AnnotatedClass {}

        Function<Class<?>, Emc4jTest> getAnnotation = (cls) -> cls.getAnnotation(Emc4jTest.class);

        var bootConfig = AnnotationMapper.map(getAnnotation.apply(AnnotatedClass.class));

        assertEquals(Extension.BOOT_ID, bootConfig.getUuid());
        assertEquals(0, bootConfig.getProperties().size());
        assertEquals(0, bootConfig.getLocalClasses().size());
        assertEquals(0, bootConfig.getExportedClasses().size());
        assertEquals(0, bootConfig.getOpenExtensions().size());
        assertEquals(0, bootConfig.getSealedExtensions().size());
        assertEquals(WebEnvironment.NONE, bootConfig.getWebEnvironment());
        assertEquals(false, bootConfig.isEnableJpa());
        assertEquals(false, bootConfig.isEnableAop());
        assertEquals(true, bootConfig.isLoadDefaultScopes());
        assertNull(bootConfig.getDefaultConfig());
    }

    /**
     * Test mapping of a complex Emc4jTest annotation
     * It is a long test but it checks all the mapping logic in an easy to read way
     * the input value (the annotation) is defined at the beginning of the test
     * the output value (the mapped BootConfig) is checked step by step
     * For each level of the annotation, the corresponding part of the BootConfig is checked
     * one input field at a time
     */
    @Test
    public void test_Emc4jTest_annotation_mapping() {

        final String APP_ID = "20638225-7e56-4272-a513-000000000003";
        final String APP_EXT_ID = "20638225-7e56-4272-a513-000000000004";
        final String APP_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000005";

        final String CORE_EXT_ID = "20638225-7e56-4272-a513-000000000001";
        final String CORE_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000002";

        // the annotation to map
        @Emc4jTest(
                enableAop = true,
                enableJpa = true,
                loadDefaultScopes = false,
                webEnvironment = WebEnvironment.RANDOM_PORT,
                classes = String.class,
                properties = "prop",
                context = @Emc4jCoreContext(
                        classes = Double.class,
                        properties = "core",
                        extensions = @Emc4jExtensionContext(
                                uuid = CORE_EXT_ID,
                                classes = Float.class,
                                properties = "core-ext",
                                exportedClasses = {Byte.class},
                                extensions = @Emc4jNestedExtensionContext(
                                        uuid = CORE_NESTED_EXT_ID,
                                        classes = Exception.class,
                                        properties = "core-nested-ext",
                                        exportedClasses = {RuntimeException.class}
                                )
                        ),
                        applications = @Emc4jApplicationContext(
                                uuid = APP_ID,
                                classes = Long.class,
                                properties = "app",
                                extensions = @Emc4jExtensionContext(
                                        uuid = APP_EXT_ID,
                                        classes = Short.class,
                                        properties = "app-ext",
                                        exportedClasses = {Character.class},
                                        extensions = @Emc4jNestedExtensionContext(
                                                uuid = APP_NESTED_EXT_ID,
                                                classes = Boolean.class,
                                                properties = "app-nested-ext",
                                                exportedClasses = {Void.class}
                                        )
                                )
                        )
                ),
                defaultConfig = @Emc4jDefault(
                        enableAop = true,
                        enableJpa = true,
                        loadDefaultScopes = false,
                        webEnvironment = WebEnvironment.RANDOM_PORT,
                        classes = List.class,
                        properties = "defprop",
                        context = @Emc4jCoreContext(
                                classes = Enum.class,
                                properties = "core",
                                extensions = @Emc4jExtensionContext(
                                        uuid = CORE_EXT_ID,
                                        classes = Math.class,
                                        properties = "core-ext",
                                        exportedClasses = {Module.class},
                                        extensions = @Emc4jNestedExtensionContext(
                                                uuid = CORE_NESTED_EXT_ID,
                                                classes = ModuleLayer.class,
                                                properties = "core-nested-ext",
                                                exportedClasses = {Package.class}
                                        )
                                ),
                                applications = @Emc4jApplicationContext(
                                        uuid = APP_ID,
                                        classes = ClassLoader.class,
                                        properties = "app",
                                        extensions = @Emc4jExtensionContext(
                                                uuid = APP_EXT_ID,
                                                classes = Cloneable.class,
                                                properties = "app-ext",
                                                exportedClasses = {Locale.class},
                                                extensions = @Emc4jNestedExtensionContext(
                                                        uuid = APP_NESTED_EXT_ID,
                                                        classes = Comparable.class,
                                                        properties = "app-nested-ext",
                                                        exportedClasses = {Void.class}
                                                )
                                        )
                                )
                        )
                )

        )
        class AnnotatedClass {}

        Function<Class<?>, Emc4jTest> getAnnotation = (cls) -> cls.getAnnotation(Emc4jTest.class);

        // the mapped value
        var bootConfig = AnnotationMapper.map(getAnnotation.apply(AnnotatedClass.class));

        // XXXXXXXXXXXXXXXX boot context assertions XXXXXXXXXXXXXXXXXX
        assertEquals(Extension.BOOT_ID, bootConfig.getUuid());
        assertEquals(true, bootConfig.isEnableAop());
        assertEquals(true, bootConfig.isEnableJpa());
        assertEquals(false, bootConfig.isLoadDefaultScopes());
        assertEquals(WebEnvironment.RANDOM_PORT, bootConfig.getWebEnvironment());

        assertEquals(1, bootConfig.getLocalClasses().size());
        assertEquals(String.class, bootConfig.getLocalClasses().get(0));

        assertEquals(1, bootConfig.getProperties().size());
        assertEquals("prop", bootConfig.getProperties().get(0));

        // XXXXXXXXXXXXXXXX core context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(bootConfig.getSealedExtensions());
        assertEquals(1, bootConfig.getSealedExtensions().size());
        assertTrue(bootConfig.getSealedExtensions().containsKey(Extension.ROOT_ID));

        var coreContext = bootConfig.getSealedExtensions().get(Extension.ROOT_ID);
        assertEquals(Extension.ROOT_ID, coreContext.getUuid());

        // XXXXXXXXXXXXXXXX core extension context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(coreContext.getOpenExtensions());
        assertEquals(1, coreContext.getOpenExtensions().size());

        var coreExtId = UUID.fromString(CORE_EXT_ID);
        assertTrue(coreContext.getOpenExtensions().containsKey(coreExtId));

        var coreExtContext = coreContext.getOpenExtensions().get(coreExtId);
        assertEquals(coreExtId, coreExtContext.getUuid());

        // XXXXXXXXXX core nested extension context assertions XXXXXXXXXXXX
        assertNotNull(coreExtContext.getOpenExtensions());
        assertEquals(1, coreExtContext.getOpenExtensions().size());

        var coreNestedExtId = UUID.fromString(CORE_NESTED_EXT_ID);
        assertTrue(coreExtContext.getOpenExtensions().containsKey(coreNestedExtId));

        var coreNestedExtContext = coreExtContext.getOpenExtensions().get(coreNestedExtId);
        assertEquals(coreNestedExtId, coreNestedExtContext.getUuid());

        // XXXXXXXXXXXXXXXX application context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(coreContext.getSealedExtensions());
        assertEquals(1, coreContext.getSealedExtensions().size());

        var appId = UUID.fromString(APP_ID);
        assertTrue(coreContext.getSealedExtensions().containsKey(appId));

        var appContext = coreContext.getSealedExtensions().get(appId);
        assertEquals(appId, appContext.getUuid());

        // XXXXXXXXXXXXXXXX application extension context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(appContext.getOpenExtensions());
        assertEquals(1, appContext.getOpenExtensions().size());

        var appExtId = UUID.fromString(APP_EXT_ID);
        assertTrue(appContext.getOpenExtensions().containsKey(appExtId));

        var appExtContext = appContext.getOpenExtensions().get(appExtId);
        assertEquals(appExtId, appExtContext.getUuid());

        // XXXXXXXXXX application nested extension context assertions XXXXXXXXXXXX
        assertNotNull(appExtContext.getOpenExtensions());
        assertEquals(1, appExtContext.getOpenExtensions().size());

        var appNestedExtId = UUID.fromString(APP_NESTED_EXT_ID);
        assertTrue(appExtContext.getOpenExtensions().containsKey(appNestedExtId));

        var appNestedExtContext = appExtContext.getOpenExtensions().get(appNestedExtId);
        assertEquals(appNestedExtId, appNestedExtContext.getUuid());


        // XXXXXXXXXXXXXXXX default config assertions XXXXXXXXXXXXXXXXXX

        assertNotNull(bootConfig.getDefaultConfig());
        assertEquals(Extension.BOOT_ID, bootConfig.getDefaultConfig().getUuid());
        assertEquals(1, bootConfig.getDefaultConfig().getLocalClasses().size());

    }

}
