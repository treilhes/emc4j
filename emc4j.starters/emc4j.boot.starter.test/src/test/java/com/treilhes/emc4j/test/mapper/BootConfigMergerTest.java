package com.treilhes.emc4j.test.mapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

class BootConfigMergerTest {

    @Test
    public void test_Emc4jTest_annotation_mapping() {

        final String APP_ID = "20638225-7e56-4272-a513-000000000003";
        final String APP_EXT_ID = "20638225-7e56-4272-a513-000000000004";
        final String APP_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000005";

        final String CORE_EXT_ID = "20638225-7e56-4272-a513-000000000001";
        final String CORE_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000002";

        final String NEW_CORE_EXT_ID = "20638225-7e56-4272-a513-000000000010";
        final String NEW_CORE_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000011";

        // the annotation to map
        @Emc4jTest(
                enableAop = false,
                enableJpa = false,
                loadDefaultScopes = false,
                webEnvironment = WebEnvironment.DEFINED_PORT,
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
                        loadDefaultScopes = true,
                        webEnvironment = WebEnvironment.RANDOM_PORT,
                        classes = List.class,
                        properties = "defprop",
                        context = @Emc4jCoreContext(
                                classes = Enum.class,
                                properties = "core",
                                extensions = {
                                        @Emc4jExtensionContext( // will be merged
                                            uuid = CORE_EXT_ID,
                                            classes = Math.class,
                                            properties = "core-ext",
                                            exportedClasses = {Module.class},
                                            extensions = {
                                                    @Emc4jNestedExtensionContext( // will be merged
                                                        uuid = CORE_NESTED_EXT_ID,
                                                        classes = ModuleLayer.class,
                                                        properties = "core-nested-ext",
                                                        exportedClasses = {Package.class}
                                                    ),
                                                    @Emc4jNestedExtensionContext( // will be added
                                                        uuid = NEW_CORE_NESTED_EXT_ID,
                                                        classes = ModuleLayer.class,
                                                        properties = "core-nested-ext",
                                                        exportedClasses = {Package.class}
                                                    )
                                            }
                                        ),
                                        @Emc4jExtensionContext( // will be added
                                            uuid = NEW_CORE_EXT_ID,
                                            classes = Math.class,
                                            properties = "new-core-ext",
                                            exportedClasses = {Module.class},
                                            extensions = @Emc4jNestedExtensionContext(
                                                    uuid = CORE_NESTED_EXT_ID,
                                                    classes = ModuleLayer.class,
                                                    properties = "core-nested-ext",
                                                    exportedClasses = {Package.class}
                                            )
                                        )
                                },
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
        bootConfig = BootConfigMerger.merge(bootConfig);

        // XXXXXXXXXXXXXXXX boot context assertions XXXXXXXXXXXXXXXXXX
        assertEquals(Extension.BOOT_ID, bootConfig.getUuid());
        assertEquals(true, bootConfig.isEnableAop()); // false | true (from default)
        assertEquals(true, bootConfig.isEnableJpa()); // false | true (from default)
        assertEquals(true, bootConfig.isLoadDefaultScopes()); // false | true (from default)
        assertEquals(WebEnvironment.DEFINED_PORT, bootConfig.getWebEnvironment()); // DEFINED_PORT over RANDOM_PORT (from default)

        assertEquals(2, bootConfig.getLocalClasses().size()); // merged classes String + List
        assertTrue(bootConfig.getLocalClasses().contains(String.class));
        assertTrue(bootConfig.getLocalClasses().contains(List.class));

        assertEquals(2, bootConfig.getProperties().size()); // merged properties prop + defprop
        assertTrue(bootConfig.getProperties().contains("prop"));
        assertTrue(bootConfig.getProperties().contains("defprop"));

        // XXXXXXXXXXXXXXXX core context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(bootConfig.getSealedExtensions());
        assertEquals(1, bootConfig.getSealedExtensions().size());
        assertTrue(bootConfig.getSealedExtensions().containsKey(Extension.ROOT_ID));

        var coreContext = bootConfig.getSealedExtensions().get(Extension.ROOT_ID);
        assertEquals(Extension.ROOT_ID, coreContext.getUuid());

        // XXXXXXXXXXXXXXXX core extension context assertions XXXXXXXXXXXXXXXXXX
        assertNotNull(coreContext.getOpenExtensions());
        assertEquals(2, coreContext.getOpenExtensions().size()); // merged core ext + new core ext

        var coreExtId = UUID.fromString(CORE_EXT_ID);
        assertTrue(coreContext.getOpenExtensions().containsKey(coreExtId));
        var newCoreExtId = UUID.fromString(NEW_CORE_EXT_ID);
        assertTrue(coreContext.getOpenExtensions().containsKey(newCoreExtId));

        var coreExtContext = coreContext.getOpenExtensions().get(coreExtId);
        assertEquals(coreExtId, coreExtContext.getUuid());

        // XXXXXXXXXX core nested extension context assertions XXXXXXXXXXXX
        assertNotNull(coreExtContext.getOpenExtensions());
        assertEquals(2, coreExtContext.getOpenExtensions().size()); // merged core nested ext + new core nested ext

        var coreNestedExtId = UUID.fromString(CORE_NESTED_EXT_ID);
        assertTrue(coreExtContext.getOpenExtensions().containsKey(coreNestedExtId));

        var newCoreNestedExtId = UUID.fromString(NEW_CORE_NESTED_EXT_ID);
        assertTrue(coreExtContext.getOpenExtensions().containsKey(newCoreNestedExtId));


    }


}
