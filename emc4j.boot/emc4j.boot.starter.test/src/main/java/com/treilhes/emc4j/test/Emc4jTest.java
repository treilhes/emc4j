package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.treilhes.emc4j.boot.api.context.Application;
import com.treilhes.emc4j.boot.api.context.ApplicationInstance;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstanceSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.Primary;
import com.treilhes.emc4j.test.Emc4jExtension.Emc4jTestContextBootstrapper;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@BootstrapWith(Emc4jTestContextBootstrapper.class)
@ExtendWith({
    MockitoExtension.class,
    SpringExtension.class,
    Emc4jExtension.class
})
public @interface Emc4jTest {
    String[] properties() default {};
    boolean loadDefaultScopes() default true;

    @ApplicationSingleton
    @Primary
    public static class Application1Bean implements Application {
    }

    @ApplicationInstanceSingleton
    @Primary
    public static class Application1InstanceBean implements ApplicationInstance {
    }

    @ApplicationSingleton
    public static class Application2Bean implements Application {
    }

    @ApplicationInstanceSingleton
    public static class Application2InstanceBean implements ApplicationInstance {
    }
}
