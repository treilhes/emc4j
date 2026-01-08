package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.treilhes.emc4j.boot.aop.AopBootClasses;
import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.loader.ExtensionContextConfigClasses;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;
import com.treilhes.emc4j.boot.api.loader.extension.RootExtension;
import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.boot.context.ContextBootClasses;
import com.treilhes.emc4j.boot.context.impl.ContextManagerImpl;
import com.treilhes.emc4j.boot.context.impl.EmContextFactory;
import com.treilhes.emc4j.boot.context.impl.EmContextImpl;
import com.treilhes.emc4j.boot.jpa.JpaBootClasses;
import com.treilhes.emc4j.boot.web.WebBootClasses;

public class Emc4jExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final static Logger logger = LoggerFactory.getLogger(Emc4jExtension.class);
    private final static Namespace EMC4J = create("com.treilhes.emc4j");

    // This constructor is invoked by JUnit Jupiter via reflection or ServiceLoader
    @SuppressWarnings("unused")
    public Emc4jExtension() {
    }


    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
    }


    /**
     * Callback that is invoked <em>before</em> each test is invoked.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeEach(final ExtensionContext context) {

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
//        var root = FxToolkit.toolkitContext().getRegisteredStage().getScene().getRoot();
//        if (root instanceof Pane r) {
//            Platform.runLater(() -> {
//                r.getChildren().clear();
//            });
//            //r.getChildren().clear();
//        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Check if the parameter is supported, e.g., by type or annotation
//        var type = parameterContext.getParameter().getType();
//        return type == Stage.class || type == StageBuilder.class;
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Provide the instance of the parameter
        var type = parameterContext.getParameter().getType();

//        if (type == Stage.class) {
//            return FxToolkit.toolkitContext().getRegisteredStage();
//        }
//
//        if (type == StageBuilder.class) {
//            var context = Emc4jContextLoader.testContextHolder.get();
//            var builder = context.getBean(StageBuilder.class);
//            builder.stage(FxToolkit.toolkitContext().getRegisteredStage());
//
//            return builder;
//        }

        return null;
    }

    public static class Emc4jTestContextBootstrapper extends SpringBootTestContextBootstrapper {

        private Map<Class<?>, MergedAnnotation<Emc4jTest>> testClassAnnotationsCache = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
            return Emc4jContextLoader.class;
        }

        @Override
        protected String[] getProperties(Class<?> testClass) {
            var annotation = Emc4jAnnotationCache.get(testClass);
            var properties = annotation.properties();
            return properties;
        }

        @Override
        protected @Nullable WebEnvironment getWebEnvironment(Class<?> testClass) {
            var annotations = Emc4jAnnotationCache.get(testClass);
            return annotations.webEnvironment();
        }

        @Override
        protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
            // Do not lookup to a config class, use directly the classes from the annotation
            return mergedConfig.getClasses();
        }

    }

    public static class Emc4jContextLoader extends AbstractContextLoader {

        protected static final ThreadLocal<com.treilhes.emc4j.boot.api.context.EmContext> testContextHolder = new ThreadLocal<>();

        private Class<?> testClass;
        private Emc4jTest annotation;

        public Emc4jContextLoader() {
            super();
        }

        @Override
        protected String[] generateDefaultLocations(Class<?> clazz) {
            this.testClass = clazz;
            this.annotation = Emc4jAnnotationCache.get(clazz);

            return super.generateDefaultLocations(clazz);
        }

        @Override
        public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
            EmContextImpl.applicationScope.clear();
//            var triage = new ContextManagerImpl.ClassTriageExecutor();
//
//            var contextId = SealedExtension.ROOT_ID;
//            var parent = Mockito.mock(ExtensionContext.class);
//            var loader = Mockito.mock(ServiceLoader.class);
//            var extensionDefinition = Mockito.mock(LoadableContent.class);
//            var extension = Mockito.mock(RootExtension.class);
//            var layer = Mockito.mock(Layer.class);
//            var layerManager = Mockito.mock(ModuleLayerManager.class);
//            // we want the context manager to return the same context everytime
//
//
//
//            //var bootstraper = new ContextBootstraper(layerManager, contextManager);
//
//            when(layer.getId()).thenReturn(contextId);
//            when(layer.getModuleLayer()).thenReturn(ModuleLayer.boot());
//            when(layerManager.get(any())).thenReturn(layer);
//            when(loader.loadService(layer, Extension.class)).thenReturn(Set.of(extension));
//
//            when(extension.getId()).thenReturn(contextId);


            var classes = new ArrayList<>(List.of(mergedConfig.getClasses()));

            //@formatter:off
            classes.addAll(List.of(
                    // FIXME MockitoPostProcessor.class generates:NoSuchMethodException: org.springframework.boot.test.mock.mockito.MockitoPostProcessor.<init>()
                    //MockitoPostProcessor.class,

                    Emc4jTestConfig.class,

                    // application beans for default scopes
                    Emc4jTest.Application1Bean.class,
                    Emc4jTest.Application1InstanceBean.class,
                    Emc4jTest.Application2Bean.class,
                    Emc4jTest.Application2InstanceBean.class
                    ));

            classes.addAll(new ContextBootClasses().classes().stream()
                    // exclude ContextManagerImpl to use our custom one
                    .filter(cls -> cls != ContextManagerImpl.class)
                    .toList()
                );

            if (annotation.webEnvironment() != WebEnvironment.NONE) {
                classes.addAll(new WebBootClasses().classes());
            }
            if (annotation.enableAop()) {
                classes.addAll(new AopBootClasses().classes());
            }
            if (annotation.enableJpa()) {
                classes.addAll(new JpaBootClasses().classes());
            }

            classes.addAll(List.of(annotation.classes()));
            //@formatter:on

//            when(extension.localContextClasses()).thenReturn(classes);
            //@formatter:on
            //var ctx = bootstraper.create(parent, extensionDefinition, List.of(contextManager), null, loader);

            //var ctx = BootContext.create(classes, new String[0]);

            WebApplicationType webAppType = annotation.webEnvironment() != WebEnvironment.NONE
                    ? WebApplicationType.SERVLET
                    : WebApplicationType.NONE;

            var bootContext = createBootContext(mergedConfig, classes, webAppType);

            var coreContextAnnotation = annotation.context();

            if (AnnotationUtils.isSet(coreContextAnnotation)) {

                var contextManager = bootContext.getBean(ContextManager.class);
                var coreContext = Helper.createCoreContext(bootContext, coreContextAnnotation, contextManager);

                if (coreContextAnnotation.extensions().length > 0) {
                    for (var coreExtensionAnnotation:coreContextAnnotation.extensions()) {
                        var coreExtensionContext = Helper.createExtensionContext(coreContext, coreExtensionAnnotation, contextManager);

                        if (coreExtensionAnnotation.extensions().length > 0) {
                            for (var coreNestedExtensionAnnotation:coreExtensionAnnotation.extensions()) {
                                var coreNestedExtensionContext = Helper.createNestedExtensionContext(coreExtensionContext, coreNestedExtensionAnnotation, contextManager);
                            }
                        }
                    }
                }

                if (coreContextAnnotation.applications().length > 0) {
                    for (var applicationAnnotation:coreContextAnnotation.applications()) {
                        var applicationContext = Helper.createApplicationContext(coreContext, applicationAnnotation, contextManager);

                        if (applicationAnnotation.extensions().length > 0) {
                            for (var applicationExtensionAnnotation:applicationAnnotation.extensions()) {
                                var applicationExtensionContext = Helper.createExtensionContext(applicationContext, applicationExtensionAnnotation, contextManager);

                                if (applicationExtensionAnnotation.extensions().length > 0) {
                                    for (var applicationNestedExtensionAnnotation:applicationExtensionAnnotation.extensions()) {
                                        var applicationNestedExtensionContext = Helper.createNestedExtensionContext(applicationExtensionContext, applicationNestedExtensionAnnotation, contextManager);
                                    }
                                }

                            }
                        }
                    }
                }
            }
            if (annotation.loadDefaultScopes()) {
                // set the current scopes
                var appBean = bootContext.getBean(Emc4jTest.Application1Bean.class);
                logger.info("Loaded Application Bean: {}", appBean);
                var instanceBean = bootContext.getBean(Emc4jTest.Application1InstanceBean.class);
                logger.info("Loaded Application Instance Bean: {}", instanceBean);

            }

            testContextHolder.set(bootContext);

            return bootContext;
        }


        private EmContextImpl createBootContext(MergedContextConfiguration mergedConfig, ArrayList<Class<?>> classes,
                WebApplicationType webAppType) {
            var ctx = new EmContextImpl(Extension.BOOT_ID, webAppType);
            var factory = new EmTestContextFactory(this.annotation);
            var contextManager = new ContextManagerImpl(ctx, factory);
            factory.setContextManager(contextManager);

            ctx.registerSingleton(contextManager);
            ctx.register(classes.toArray(new Class[0]));

            var env = ctx.getEnvironment();
            env.setActiveProfiles(mergedConfig.getActiveProfiles());

            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, mergedConfig.getPropertySourceProperties());
            TestPropertySourceUtils.addPropertySourcesToEnvironment(
                    ctx,
                    mergedConfig.getPropertySourceDescriptors()
                );

            for (var customizer : mergedConfig.getContextCustomizers()) {
                customizer.customizeContext(ctx, mergedConfig);
            }


            ctx.refresh();
            return ctx;
        }

        @Override
        protected String[] getResourceSuffixes() {
            return new String[] { "-context.xml", "Context.groovy" };
        }

        @Override
        protected String getResourceSuffix() {
            throw new IllegalStateException();
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    //@AutoConfigureMockMvc
    static class Emc4jTestConfig {

//        @Bean
//        @ConditionalOnMissingBean
//        public DataSource dataSource() {
//            var dataSource = new DriverManagerDataSource();
//            dataSource.setDriverClassName("org.h2.Driver");
//            dataSource.setUrl("jdbc:h2:mem:testdb"); // Pour une base en mémoire ou file:./data/testdb pour fichier
//            dataSource.setUsername("sa");
//            dataSource.setPassword(""); // H2 par défaut n'a pas de mot de passe pour l'utilisateur 'sa'
//
//            return dataSource;
//        }
//
//        @Bean
//        //@ConditionalOnBean(name = "servletContext")
//        @ConditionalOnMissingBean
//        public JpaVendorAdapter jpaVendorAdapter() {
//            var adapter = new HibernateJpaVendorAdapter();
//            adapter.setGenerateDdl(true); // Générer automatiquement le schéma de base de données
//            adapter.setShowSql(true); // Afficher les requêtes SQL dans la console
//            adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect"); // Utilisation de H2
//            return adapter;
//        }

        @Bean
        @ConditionalOnBean(name = "servletContext")
        public SwaggerUiConfigProperties swaggerUiConfigProperties() {
            return new SwaggerUiConfigProperties();
        }

        @Bean
        @ConditionalOnBean(name = "servletContext")
        public SwaggerUiOAuthProperties swaggerUiOAuthProperties() {
            return new SwaggerUiOAuthProperties();
        }

        @Bean
        @ConditionalOnBean(name = "servletContext")
        public ObjectMapperProvider objectMapperProvider(SpringDocConfigProperties config) {
            return new ObjectMapperProvider(config);
        }

    }

    static class Emc4jAnnotationCache {
        private static Map<Class<?>, Emc4jTest> testClassAnnotationsCache = new java.util.concurrent.ConcurrentHashMap<>();

        public static Emc4jTest get(Class<?> testClass) {
            return testClassAnnotationsCache.computeIfAbsent(testClass, cls ->
                cls.getAnnotation(Emc4jTest.class)
            );
        }
    }

    static class EmTestContextFactory implements EmContextFactory {

        final Emc4jTest annotation;
        ContextManagerImpl contextManager;

        public EmTestContextFactory(Emc4jTest annotation) {
            super();
            this.annotation = annotation;
        }

        void setContextManager(ContextManagerImpl contextManager) {
            this.contextManager = contextManager;
        }

        @Override
        public EmContext create(ApplicationContext parent, UUID uuid, ClassLoader loader, Collection<Class<?>> classes,
                Collection<Class<?>> deportedClasses, Collection<Object> singletonInstances,
                WebApplicationType webApplicationType) {

            var context = new EmContextImpl(uuid, loader, webApplicationType) {

                /**
                 * Getting a bean from a specific layer is not possible during tests as all layers
                 * are merged into a single junit layer. To allow tests to run, we fallback to checking the origin layer
                 * by inspecting the test configuration to find the context uuid registering the layerClass
                 * <br>NOTE:</br> this is only for testing purposes and should not be used in production code.
                 * </br></br>
                 * Original doc: {@inheritDoc}
                 */
                @Override
                public <T> T getLayerBean(Class<?> layerClass, Class<T> cls) {

                    var uuid = Helper.findContextForClass(annotation, layerClass);

                    var layerContext = contextManager.get(uuid);

                    if (layerContext == null) {
                        return getBean(cls);
                    }

                    return layerContext.getBean(cls);
                }

            };

            context.setParent(parent);
            context.register(classes.toArray(Class<?>[]::new));
            context.deport(deportedClasses.toArray(new Class<?>[0]));

            if (singletonInstances != null) {
                singletonInstances.forEach(context::registerSingleton);
            }

            return context;
        }

    }

    class AnnotationUtils {

        static boolean isSet(Emc4jCoreContext annotation) {
            return annotation != null && (annotation.classes().length > 0 || annotation.extensions().length > 0
                    || annotation.applications().length > 0);
        }

        static boolean isSet(Emc4jApplicationContext annotation) {
            return annotation != null && (annotation.classes().length > 0 || annotation.extensions().length > 0);
        }

        static boolean isSet(Emc4jNestedExtensionContext annotation) {
            return annotation != null && (annotation.classes().length > 0 || annotation.exportedClasses().length > 0);
        }

        static boolean isSet(Emc4jExtensionContext annotation) {
            return annotation != null && (annotation.classes().length > 0 || annotation.extensions().length > 0
                    || annotation.exportedClasses().length > 0);
        }

    }

    class Helper {


        static EmContext createCoreContext(EmContext parentContext, Emc4jCoreContext annotation, ContextManager contextManager) {

            var id = annotation.uuid().isBlank() ? SealedExtension.ROOT_ID : UUID.fromString(annotation.uuid());
            var exportedByExt = Arrays.stream(annotation.extensions()).map(e -> e.exportedClasses()).flatMap(Arrays::stream).toList();
            var isSealed = false;
            var frameworkExtensionClasses = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
                    .values().stream().flatMap(c -> c.classes().stream()).toList();

            var mockExtension = Mockito.mock(RootExtension.class);
            when(mockExtension.getId()).thenReturn(id);
            when(mockExtension.getParentId()).thenReturn(parentContext.getUuid());
            when(mockExtension.localContextClasses()).thenReturn(List.of(annotation.classes()));

            var configuration = new ContextConfiguration();
            configuration.setId(id);
            configuration.setParentContext(parentContext);
            configuration.setSealed(isSealed);
            configuration.setLayer(null);
            configuration.addClasses(frameworkExtensionClasses);
            configuration.addClasses(List.of(annotation.classes()));
            configuration.addChildrenClasses(exportedByExt);
            configuration.addSingletonInstances(List.of(mockExtension));
            configuration.setProgressListener(null);

            return contextManager.create(configuration);
        }

        static EmContext createApplicationContext(EmContext parentContext, Emc4jApplicationContext annotation, ContextManager contextManager) {

            var id = annotation.uuid().isBlank() ? UUID.randomUUID() : UUID.fromString(annotation.uuid());
            var exportedByExt = Arrays.stream(annotation.extensions()).map(e -> e.exportedClasses()).flatMap(Arrays::stream).toList();
            var isSealed = true;
            var frameworkExtensionClasses = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
                    .values().stream().flatMap(c -> c.classes().stream()).toList();

            var mockExtension = Mockito.mock(SealedExtension.class);
            when(mockExtension.getId()).thenReturn(id);
            when(mockExtension.getParentId()).thenReturn(parentContext.getUuid());
            when(mockExtension.localContextClasses()).thenReturn(List.of(annotation.classes()));

            var configuration = new ContextConfiguration();
            configuration.setId(id);
            configuration.setParentContext(parentContext);
            configuration.setSealed(isSealed);
            configuration.setLayer(null);
            configuration.addClasses(frameworkExtensionClasses);
            configuration.addClasses(List.of(annotation.classes()));
            configuration.addChildrenClasses(exportedByExt);
            configuration.addSingletonInstances(List.of(mockExtension));
            configuration.setProgressListener(null);

            return contextManager.create(configuration);
        }

        static EmContext createExtensionContext(EmContext parentContext, Emc4jExtensionContext annotation,
                ContextManager contextManager) {

            var id = annotation.uuid().isBlank() ? UUID.randomUUID() : UUID.fromString(annotation.uuid());
            var exportedByExt = Arrays.stream(annotation.extensions()).map(e -> e.exportedClasses()).flatMap(Arrays::stream).toList();
            var isSealed = false;
            var frameworkExtensionClasses = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
                    .values().stream().flatMap(c -> c.classes().stream()).toList();

            var mockExtension = Mockito.mock(OpenExtension.class);
            when(mockExtension.getId()).thenReturn(id);
            when(mockExtension.getParentId()).thenReturn(parentContext.getUuid());
            when(mockExtension.localContextClasses()).thenReturn(List.of(annotation.classes()));
            when(mockExtension.exportedContextClasses()).thenReturn(List.of(annotation.exportedClasses()));

            var configuration = new ContextConfiguration();
            configuration.setId(id);
            configuration.setParentContext(parentContext);
            configuration.setSealed(isSealed);
            configuration.setLayer(null);
            configuration.addClasses(frameworkExtensionClasses);
            configuration.addClasses(List.of(annotation.classes()));
            configuration.addChildrenClasses(exportedByExt);
            configuration.addSingletonInstances(List.of(mockExtension));
            configuration.setProgressListener(null);

            return contextManager.create(configuration);
        }

        static EmContext createNestedExtensionContext(EmContext parentContext, Emc4jNestedExtensionContext annotation,
                ContextManager contextManager) {

            var id = annotation.uuid().isBlank() ? UUID.randomUUID() : UUID.fromString(annotation.uuid());
            List<Class<?>> exportedByExt = List.of();
            var isSealed = false;
            var frameworkExtensionClasses = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
                    .values().stream().flatMap(c -> c.classes().stream()).toList();

            var mockExtension = Mockito.mock(OpenExtension.class);
            when(mockExtension.getId()).thenReturn(id);
            when(mockExtension.getParentId()).thenReturn(parentContext.getUuid());
            when(mockExtension.localContextClasses()).thenReturn(List.of(annotation.classes()));
            when(mockExtension.exportedContextClasses()).thenReturn(List.of(annotation.exportedClasses()));

            var configuration = new ContextConfiguration();
            configuration.setId(id);
            configuration.setParentContext(parentContext);
            configuration.setSealed(isSealed);
            configuration.setLayer(null);
            configuration.addClasses(frameworkExtensionClasses);
            configuration.addClasses(List.of(annotation.classes()));
            configuration.addChildrenClasses(exportedByExt);
            configuration.addSingletonInstances(List.of(mockExtension));
            configuration.setProgressListener(null);

            return contextManager.create(configuration);
        }


        /**
         * Find classes in configured context
         * Ugly, but it will do for now
         * @param annotation the configuration annotation
         * @param layerClass the class to search for
         * @return the context UUID if found, null otherwise
         */
        static UUID findContextForClass(Emc4jTest annotation, Class<?> layerClass) {

            //search in root context
            for (var cls:annotation.context().classes()) {
                if (cls.equals(layerClass)) {
                    return UUID.fromString(annotation.context().uuid());
                }
            }


            for (var ext:annotation.context().extensions()) {

                //search in extensions
                for (var cls:ext.classes()) {
                    if (cls.equals(layerClass)) {
                        return UUID.fromString(ext.uuid());
                    }
                }

                //search in extension children
                for (var exyOfExt:ext.extensions()) {
                    for (var cls:exyOfExt.classes()) {
                        if (cls.equals(layerClass)) {
                            return UUID.fromString(exyOfExt.uuid());
                        }
                    }

                }
            }

            for (var app:annotation.context().applications()) {

                //search in extensions
                for (var cls:app.classes()) {
                    if (cls.equals(layerClass)) {
                        return UUID.fromString(app.uuid());
                    }
                }

                //search in extension children
                for (var exyOfExt:app.extensions()) {
                    for (var cls:exyOfExt.classes()) {
                        if (cls.equals(layerClass)) {
                            return UUID.fromString(exyOfExt.uuid());
                        }
                    }

                    for (var exyOfExtOfExt:exyOfExt.extensions()) {
                        for (var cls:exyOfExtOfExt.classes()) {
                            if (cls.equals(layerClass)) {
                                return UUID.fromString(exyOfExtOfExt.uuid());
                            }
                        }
                    }
                }
            }


            return null;
        }
    }


}
