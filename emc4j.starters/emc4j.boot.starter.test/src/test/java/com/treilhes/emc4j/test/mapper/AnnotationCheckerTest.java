package com.treilhes.emc4j.test.mapper;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.junit.Test;

import com.treilhes.emc4j.test.Emc4jApplicationContext;
import com.treilhes.emc4j.test.Emc4jCoreContext;
import com.treilhes.emc4j.test.Emc4jExtensionContext;
import com.treilhes.emc4j.test.Emc4jNestedExtensionContext;
import com.treilhes.emc4j.test.Emc4jTest;

public class AnnotationCheckerTest {

    @Test
    public void empty_Emc4jCoreContext_annotation_must_be_detected() {

        @Emc4jTest
        class AnnotatedClass {}

        Function<Class<?>, Emc4jCoreContext> getAnnotation = (cls) -> cls.getAnnotation(Emc4jTest.class).context();

        assertFalse(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedClass.class)));

    }

    @Test
    public void not_empty_Emc4jCoreContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(classes = Object.class))
        class AnnotatedWithClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(properties = "x=y"))
        class AnnotatedWithPropertyClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext))
        class AnnotatedWithApplicationClass {}
        @Emc4jTest(context = @Emc4jCoreContext(extensions = @Emc4jExtensionContext))
        class AnnotatedWithExtensionClass {}

        Function<Class<?>, Emc4jCoreContext> getAnnotation = (cls) -> cls.getAnnotation(Emc4jTest.class).context();

        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithPropertyClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithApplicationClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithExtensionClass.class)));

    }

    @Test
    public void empty_Emc4jApplicationContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext))
        class AnnotatedClass {}

        Function<Class<?>, Emc4jApplicationContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0];

        assertFalse(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedClass.class)));

    }

    @Test
    public void not_empty_Emc4jApplicationContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(uuid = "x")))
        class AnnotatedWithUuidClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(classes = Object.class)))
        class AnnotatedWithClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(properties = "x=y")))
        class AnnotatedWithPropertyClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext)))
        class AnnotatedWithExtensionClass {}

        Function<Class<?>, Emc4jApplicationContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0];

        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithUuidClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithPropertyClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithExtensionClass.class)));

    }

    @Test
    public void empty_Emc4jExtensionContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext)))
        class AnnotatedClass {}

        Function<Class<?>, Emc4jExtensionContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0].extensions()[0];

        assertFalse(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedClass.class)));

    }

    @Test
    public void not_empty_Emc4jExtensionContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(uuid = "x"))))
        class AnnotatedWithUuidClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(classes = Object.class))))
        class AnnotatedWithClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(exportedClasses = Object.class))))
        class AnnotatedWithExportedClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(properties = "x=y"))))
        class AnnotatedWithPropertyClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext))))
        class AnnotatedWithExtensionClass {}

        Function<Class<?>, Emc4jExtensionContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0].extensions()[0];

        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithUuidClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithExportedClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithPropertyClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithExtensionClass.class)));

    }

    @Test
    public void empty_Emc4jNestedExtensionContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext))))
        class AnnotatedClass {}

        Function<Class<?>, Emc4jNestedExtensionContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0].extensions()[0].extensions()[0];

        assertFalse(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedClass.class)));

    }

    @Test
    public void not_empty_Emc4jNestedExtensionContext_annotation_must_be_detected() {

        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext(uuid = "x")))))
        class AnnotatedWithUuidClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext(classes = Object.class)))))
        class AnnotatedWithClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext(exportedClasses = Object.class)))))
        class AnnotatedWithExportedClassClass {}
        @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext(properties = "x=y")))))
        class AnnotatedWithPropertyClass {}

        Function<Class<?>, Emc4jNestedExtensionContext> getAnnotation =
                (cls) -> cls.getAnnotation(Emc4jTest.class).context().applications()[0].extensions()[0].extensions()[0];

        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithUuidClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithExportedClassClass.class)));
        assertTrue(AnnotationChecker.isSet(getAnnotation.apply(AnnotatedWithPropertyClass.class)));

    }

}
