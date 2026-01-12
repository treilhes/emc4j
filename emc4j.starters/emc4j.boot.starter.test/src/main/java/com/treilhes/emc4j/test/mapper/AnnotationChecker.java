package com.treilhes.emc4j.test.mapper;

import com.treilhes.emc4j.test.Emc4jApplicationContext;
import com.treilhes.emc4j.test.Emc4jCoreContext;
import com.treilhes.emc4j.test.Emc4jDefault;
import com.treilhes.emc4j.test.Emc4jExtensionContext;
import com.treilhes.emc4j.test.Emc4jNestedExtensionContext;
import com.treilhes.emc4j.test.Emc4jTest;

public class AnnotationChecker {

    @Emc4jTest
    private class EmptyEmc4jTest{}

    private static final Emc4jTest emptyEmc4jTestAnnotation = EmptyEmc4jTest.class.getAnnotation(Emc4jTest.class);
    private static final Emc4jCoreContext emptyEmc4jCoreContextAnnotation = emptyEmc4jTestAnnotation.context();
    private static final Emc4jDefault emptyEmc4jDefaultAnnotation = emptyEmc4jTestAnnotation.defaultConfig();

    @Emc4jTest(context = @Emc4jCoreContext(applications = @Emc4jApplicationContext))
    private class EmptyEmc4jApplicationContext{}

    private static final Emc4jApplicationContext emptyEmc4jApplicationContextAnnotation = EmptyEmc4jApplicationContext.class
            .getAnnotation(Emc4jTest.class).context().applications()[0];

    @Emc4jTest(context = @Emc4jCoreContext(extensions = @Emc4jExtensionContext))
    private class EmptyEmc4jExtensionContext{}

    private static final Emc4jExtensionContext emptyEmc4jExtensionContextAnnotation = EmptyEmc4jExtensionContext.class
            .getAnnotation(Emc4jTest.class).context().extensions()[0];

    @Emc4jTest(context = @Emc4jCoreContext(extensions = @Emc4jExtensionContext(extensions = @Emc4jNestedExtensionContext)))
    private class EmptyEmc4jNestedExtensionContext{}

    private static final Emc4jNestedExtensionContext emptyEmc4jNestedExtensionContextAnnotation = EmptyEmc4jNestedExtensionContext.class
            .getAnnotation(Emc4jTest.class).context().extensions()[0].extensions()[0];


    public static boolean isSet(Emc4jTest annotation) {
        return annotation != null && !emptyEmc4jTestAnnotation.equals(annotation);
    }
    public static boolean isSet(Emc4jCoreContext annotation) {
        return annotation != null && !emptyEmc4jCoreContextAnnotation.equals(annotation);
    }

    public static boolean isSet(Emc4jApplicationContext annotation) {
        return annotation != null && !emptyEmc4jApplicationContextAnnotation.equals(annotation);
    }

    public static boolean isSet(Emc4jNestedExtensionContext annotation) {
        return annotation != null && !emptyEmc4jNestedExtensionContextAnnotation.equals(annotation);
    }

    public static boolean isSet(Emc4jExtensionContext annotation) {
        return annotation != null && !emptyEmc4jExtensionContextAnnotation.equals(annotation);
    }

    public static boolean isSet(Emc4jDefault annotation) {
        return annotation != null && !emptyEmc4jDefaultAnnotation.equals(annotation);
    }

}