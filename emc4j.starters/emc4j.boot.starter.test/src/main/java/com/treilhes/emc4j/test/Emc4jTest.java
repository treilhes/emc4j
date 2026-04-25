package com.treilhes.emc4j.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.BootstrapWith;

import com.treilhes.emc4j.boot.api.context.Application;
import com.treilhes.emc4j.boot.api.context.ApplicationInstance;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstanceSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.Primary;
import com.treilhes.emc4j.test.Emc4jExtension.Emc4jTestContextBootstrapper;

/**
 * Main annotation for Emc4j tests. It is meta-annotated with {@link BootstrapWith} and {@link ExtendWith} to set up the test context and extensions.
 * It also provides several attributes to configure the test context, such as properties, web environment, JPA and AOP enabling, default scopes loading, classes to load, core context configuration, default configuration, and extensions to load.
 * It also defines some default application and application instance beans for testing purposes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@BootstrapWith(Emc4jTestContextBootstrapper.class)
@ExtendWith({
    MockitoExtension.class,
    Emc4jSpringExtension.class,
    Emc4jExtension.class
})
public @interface Emc4jTest {
//    public static UUID DEFAULT_EXT_UUID = UUID.randomUUID();
//    public static String DEFAULT_EXT_STRING_UUID = DEFAULT_EXT_UUID.toString();

    /**
     * Properties to set for the test context. These properties will be applied to
     * the Spring Environment of the test context, allowing you to override default
     * properties or set specific configurations for your tests.
     */
    String[] properties() default {};

    /**
     * The web environment to use for the test. This attribute allows you to specify
     * the type of web environment to set up for your tests, such as MOCK,
     * RANDOM_PORT, DEFINED_PORT, or NONE. The default is NONE, which means no web
     * environment will be set up.
     * This is the same than SpringBootTest.WebEnvironment
     */
    WebEnvironment webEnvironment() default WebEnvironment.NONE;

    /**
     * Whether to enable JPA for the test context. If set to true, JPA will be
     * enabled and configured for the test context, allowing you to use JPA
     * repositories and entities in your tests. The default is false, which means
     * JPA will not be enabled.
     */
    boolean enableJpa() default false;
    /**
     * Whether to enable AOP for the test context. If set to true, AOP will be enabled
     * and configured for the test context, allowing you to use AOP features such as
     * aspect-oriented programming in your tests. The default is false, which means AOP will not be enabled.
     */
    boolean enableAop() default false;

    /**
     * Whether to load default scopes for the test context. If set to true, default
     * scopes will be loaded and configured for the test context, allowing you to
     * use default scopes in your tests. The default is true, which means default
     * scopes will be loaded.
     * <br><br>
     * Default scopes consists of two application instance beans, all of them are
     * primary. The first application bean is of type Application1Bean and the
     * second one is of type Application2Bean. The first application instance bean
     * is of type Application1InstanceBean and the second one is of type
     * Application2InstanceBean.
     *<br><br>
     * Those classes are provided to the test context by default but loaded
     * only if loadDefaultScopes is set to true. If not, manualy requesting the beans
     * using getBean() will trigger the load.
     */
    boolean loadDefaultScopes() default true;

    /**
     * Classes to load for the test context. This attribute allows you to specify
     * additional classes to load and configure for the test context, such as
     * configuration classes, component classes, or any other classes that are relevant
     * for your tests. The default is an empty array, which means no additional classes will be loaded.
     */
    Class<?>[] classes() default {};

    /**
     * Core context configuration for the test context. A core context is the first
     * and unique child of the boot context (the boot context is the context defined
     * by this annotation). This attribute allows you to specify a core context
     * configuration for the test context, which can include various settings and
     * configurations related to the core context of Emc4j. The default is an empty
     * configuration, which means no specific core context configuration will be
     * applied.
     */
    Emc4jCoreContext context() default @Emc4jCoreContext;

    /**
     * Dedicated to annotation that aims to extends Emc4jTest, it allows to set a
     * default configuration for the test context, which can include various
     * settings and configurations related to the core context of Emc4j. The default
     * is an empty configuration, which means no specific default configuration will
     * be applied. It exposes the same attributes as Emc4jTest and will be merged to
     * the configuration provided by the annotation that extends Emc4jTest. This
     * allows to set a default configuration for the test context, which can be
     * overridden by the attributes of the annotation that extends Emc4jTest. <br>
     * <br>
     */
    Emc4jDefault defaultConfig() default @Emc4jDefault;

    // extension configuration classes
    /**
     * Extensions to load for the test context. This attribute allows you to specify
     * additional extensions to load and configure for the test context, such as
     * custom JUnit extensions that provide additional functionality or integration
     * with other libraries or frameworks. The default is an empty array, which
     * means no additional extensions will be loaded. These extensions will be added
     * to the extensions defined in the @ExtendWith annotation. As @ExtendWith is
     * defined on the annotation itself and isn't inherited, it won't be inherited
     * by the annotation that extends Emc4jTest, so this attribute allows to set
     * extensions to load for the test context, which can be used in the annotation
     * that extends Emc4jTest.
     * Not implemented yet
     */
    //FIXME implement this feature
    Class<Extension>[] extendWith() default {};

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
