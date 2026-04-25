package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.treilhes.emc4j.boot.aop.AopBootClasses;
import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.beans.ExtensionDefinition;
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
import com.treilhes.emc4j.test.mapper.AnnotationMapper;
import com.treilhes.emc4j.test.mapper.BootConfig;
import com.treilhes.emc4j.test.mapper.BootConfigMerger;
import com.treilhes.emc4j.test.mapper.ContextConfig;

public class Emc4jExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(Emc4jExtension.class);
    private static final Namespace EMC4J = create("com.treilhes.emc4j");

    // This constructor is invoked by JUnit Jupiter via reflection or ServiceLoader
    @SuppressWarnings("unused")
    public Emc4jExtension() {
        // no-op
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // no-op
    }


    /**
     * Callback that is invoked <em>before</em> each test is invoked.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeEach(final ExtensionContext context) {
        Object testInstance = context.getRequiredTestInstance();
        var contextHolder = Emc4jContextLoader.testContextHolder.get();
        var contextMap = contextHolder;
        //var contextMap = contextHolder != null ? contextHolder.get(testInstance.getClass()) : null;

        for (Field field : testInstance.getClass().getDeclaredFields()) {
            handleEmcInject(testInstance, field, contextMap);
            handleMockEmcInject(testInstance, field, contextMap);
            handleSpyEmcInject(testInstance, field, contextMap);
        }
    }

    private void handleEmcInject(Object testInstance, Field field, Map<UUID, EmContext> contextMap) {
        if (field.isAnnotationPresent(EmcInject.class)) {
            field.setAccessible(true);

            EmcInject annotation = field.getAnnotation(EmcInject.class);

            Object dependency = getEmInjectBean(field.getType(), annotation, contextMap);

            try {
                field.set(testInstance, dependency);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleSpyEmcInject(Object testInstance, Field field, Map<UUID, EmContext> contextMap) {
        if (field.isAnnotationPresent(EmcInjectSpy.class)) {
            field.setAccessible(true);

            EmcInjectSpy annotation = field.getAnnotation(EmcInjectSpy.class);

            Object dependency = getEmInjectBean(field.getType(), annotation, contextMap);

            try {
                field.set(testInstance, Mockito.spy(dependency));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleMockEmcInject(Object testInstance, Field field, Map<UUID, EmContext> contextMap) {
        if (field.isAnnotationPresent(EmcInjectMock.class)) {
            field.setAccessible(true);

            EmcInjectMock annotation = field.getAnnotation(EmcInjectMock.class);
            Scope scope = field.getAnnotation(Scope.class);
            Primary primary = field.getAnnotation(Primary.class);

            Object dependency = setEmInjectMockBean(field.getType(), annotation, scope, primary, contextMap);

            try {
                field.set(testInstance, dependency);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
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

        var parameter = parameterContext.getParameter();
        var type = parameter.getType();
        var emcInject = parameter.getAnnotation(EmcInject.class);
        var emcMock = parameter.getAnnotation(EmcInjectMock.class);
        var emcSpy = parameter.getAnnotation(EmcInjectSpy.class);

        return EmContext.class.isAssignableFrom(type) ||
                emcInject != null ||
                emcMock != null ||
                emcSpy != null;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Provide the instance of the parameter
        var contextHolder = Emc4jContextLoader.testContextHolder.get();
        var contextMap = contextHolder;
        //var contextMap = contextHolder != null ? contextHolder.get(extensionContext.getTestClass().orElseThrow()) : null;

        System.out.println("Using contexts: " + Emc4jContextLoader.testContextHolder.get());
        System.out.println("In : " + extensionContext.getTestClass().get());
        System.out.println("method : " + extensionContext.getTestMethod().get().getName());

        var parameter = parameterContext.getParameter();
        var type = parameterContext.getParameter().getType();

        var emcInject = parameter.getAnnotation(EmcInject.class);
        if (emcInject != null) {
            return getEmInjectBean(type, emcInject, contextMap);
        }

        var emcSpy = parameter.getAnnotation(EmcInjectSpy.class);
        if (emcSpy != null) {
            return Mockito.spy(getEmInjectBean(type, emcInject, contextMap));
        }

        var emcMock = parameter.getAnnotation(EmcInjectMock.class);
        if (emcMock != null) {
            var scope = parameter.getAnnotation(Scope.class);
            var primary = parameter.getAnnotation(Primary.class);
            var mock = setEmInjectMockBean(type, emcMock, scope, primary, contextMap);
            return mock;
        }

        if (EmContext.class.isAssignableFrom(type)) {
            var context = contextMap.get(Extension.BOOT_ID);
            if (context !=null) {
                return context;
            }
        }

        return null;

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

    }

    private Object getEmInjectBean(Class<?> type, EmcInject emContext, Map<UUID, EmContext> contextMap) {
        var uuid = emContext != null && !emContext.contextId().isBlank() ? UUID.fromString(emContext.contextId()) : Extension.BOOT_ID;
        var name = emContext != null && !emContext.qualifier().isBlank() ? emContext.qualifier() : null;
        var context = contextMap.get(uuid);

        if (EmContext.class.isAssignableFrom(type)) {
            return context;
        }

        if (context == null) {
            return null;
        }

        if (name != null) {
            return context.getBean(name, type);
        }
        return context.getBean(type);
    }

    private Object getEmInjectBean(Class<?> type, EmcInjectSpy emContext, Map<UUID, EmContext> contextMap) {
        var uuid = emContext != null && !emContext.contextId().isBlank() ? UUID.fromString(emContext.contextId()) : Extension.BOOT_ID;
        var name = emContext != null && !emContext.qualifier().isBlank() ? emContext.qualifier() : null;
        var context = contextMap.get(uuid);

        if (EmContext.class.isAssignableFrom(type)) {
            return context;
        }

        if (context == null) {
            return null;
        }

        if (name != null) {
            return context.getBean(name, type);
        }
        return context.getBean(type);
    }

    private <T> T setEmInjectMockBean(Class<T> type, EmcInjectMock emContext, Scope scope, Primary primary, Map<UUID, EmContext> contextMap) {
        var uuid = emContext != null && !emContext.contextId().isBlank() ? UUID.fromString(emContext.contextId()) : Extension.BOOT_ID;
        var name = emContext != null && !emContext.qualifier().isBlank() ? emContext.qualifier() : null;
        var context = contextMap.get(uuid);

        if (context == null) {
            return null;
        }

        var customizers = new ArrayList<BeanDefinitionCustomizer>();
        if (scope != null) {
            customizers.add(bd -> bd.setScope(scope.value()));
        }
        if (primary != null) {
            customizers.add(bd -> bd.setPrimary(true));
        }

        if (name != null) {
            if (context.containsBean(name)) {
                context.removeBeanDefinition(name);
            }
            context.registerBean(name, type, () -> Mockito.mock(type), customizers.toArray(BeanDefinitionCustomizer[]::new));
            return context.getBean(name, type); // force initialization to register the mock in the context
        } else {
            context.registerBean(type, () -> Mockito.mock(type), customizers.toArray(BeanDefinitionCustomizer[]::new));
            return context.getBean(type);
        }
    }

    public static class Emc4jTestContextBootstrapper extends SpringBootTestContextBootstrapper {

        @Override
        protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
            return Emc4jContextLoader.class;
        }

        @Override
        protected String[] getProperties(Class<?> testClass) {
            var properties = Emc4jAnnotationCache.get(testClass).getProperties();

            // add the test class name as a property to prevent Spring from caching the
            // context between different test classes with the same configuration
            properties.add("testClass=" + testClass.getName());

            return properties.toArray(String[]::new);
        }

        @Override
        protected @Nullable WebEnvironment getWebEnvironment(Class<?> testClass) {
            return Emc4jAnnotationCache.get(testClass).getWebEnvironment();
        }

        @Override
        protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
            // Do not lookup to a config class, use directly the classes from the annotation
            return mergedConfig.getClasses();
        }

    }

    public static class Emc4jContextLoader extends AbstractContextLoader {

        //protected static final ThreadLocal<Map<Class<?>, Map<UUID, EmContext>>> testContextHolder = new ThreadLocal<>();
        protected static final ThreadLocal<Map<UUID, EmContext>> testContextHolder = new ThreadLocal<>();

        private BootConfig bootConfig;

        public Emc4jContextLoader() {
            super();
        }

        @Override
        protected String[] generateDefaultLocations(Class<?> clazz) {
            this.bootConfig = Emc4jAnnotationCache.get(clazz);
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

            var classes = new ArrayList<Class<?>>();

            classes.addAll(bootConfig.getLocalClasses());

            // classes from merged config (e.g. from @Import) or Spring annotations
            classes.addAll(List.of(mergedConfig.getClasses()));

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

            if (bootConfig.getWebEnvironment() != WebEnvironment.NONE) {
                classes.addAll(new WebBootClasses().classes());
            }
            if (bootConfig.isEnableAop()) {
                classes.addAll(new AopBootClasses().classes());
            }
            if (bootConfig.isEnableJpa()) {
                classes.addAll(new JpaBootClasses().classes());
            }
            if (bootConfig.isLoadDefaultScopes()) {
                classes.addAll(new JpaBootClasses().classes());
            }

            //@formatter:on

//            when(extension.localContextClasses()).thenReturn(classes);
            //@formatter:on
            //var ctx = bootstraper.create(parent, extensionDefinition, List.of(contextManager), null, loader);

            //var ctx = BootContext.create(classes, new String[0]);

            WebApplicationType webAppType = bootConfig.getWebEnvironment() != WebEnvironment.NONE
                    ? WebApplicationType.SERVLET
                    : WebApplicationType.NONE;

            var contextMap = new HashMap<UUID, EmContext>();
            var bootContext = createBootContext(mergedConfig, classes, webAppType);
            contextMap.put(Extension.BOOT_ID, bootContext);

            var contextManager = bootContext.getBean(ContextManager.class);

            if (bootConfig.getSealedExtensions().containsKey(SealedExtension.ROOT_ID)) {
                var rootConfig = bootConfig.getSealedExtensions().get(SealedExtension.ROOT_ID);
                var childMap = createChildrenContext(contextManager, bootContext, rootConfig, true);
                contextMap.putAll(childMap);
            }


            if (bootConfig.isLoadDefaultScopes()) {
                // set the current scopes
                var appBean = bootContext.getBean(Emc4jTest.Application1Bean.class);
                logger.info("Loaded Application Bean: {}", appBean);
                var instanceBean = bootContext.getBean(Emc4jTest.Application1InstanceBean.class);
                logger.info("Loaded Application Instance Bean: {}", instanceBean);

            }

            System.out.println("Previous contexts: " + testContextHolder.get());

            var ctxHolder = testContextHolder.get();

            if (ctxHolder == null) {
                ctxHolder = new HashMap<>();
            }

            testContextHolder.set(contextMap);
//          ctxHolder.put(mergedConfig.getTestClass(), contextMap);
//          testContextHolder.set(ctxHolder);

            System.out.println("AFter contexts: " + testContextHolder.get());


            return bootContext;
        }

        private EmContextImpl createBootContext(MergedContextConfiguration mergedConfig, ArrayList<Class<?>> classes,
                WebApplicationType webAppType) {
            var ctx = new EmContextImpl(Extension.BOOT_ID, webAppType);
            var factory = new EmTestContextFactory(this.bootConfig);
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


        private Map<UUID, EmContext> createChildrenContext(ContextManager contextManager, EmContext parentContext, ContextConfig config, boolean sealed) {

            Map<UUID, EmContext> contextMap = new HashMap<>();

            var id = config.getUuid();
            var exportedByExt = config.getOpenExtensions().values().stream().map(e -> e.getExportedClasses()).flatMap(List::stream).toList();
            var frameworkExtensionClasses = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
                    .values().stream().flatMap(c -> c.classes().stream()).toList();

            Extension mockExtension = null;

            if (parentContext.getUuid().equals(Extension.BOOT_ID)) {
                mockExtension = Mockito.mock(RootExtension.class);
            } else if (sealed) {
                mockExtension = Mockito.mock(SealedExtension.class);
            } else {
                var openExtension = Mockito.mock(OpenExtension.class);
                when(openExtension.exportedContextClasses()).thenReturn(config.getExportedClasses());
                mockExtension = openExtension;
            }

            var extDefinition = new ExtensionDefinition(mockExtension, Set.of());

            var configuration = new ContextConfiguration();
            configuration.setId(id);
            configuration.setParentContext(parentContext);
            configuration.setSealed(sealed);
            configuration.setLayer(null);
            configuration.addClasses(frameworkExtensionClasses);
            configuration.addClasses(config.getLocalClasses());
            configuration.addChildrenClasses(exportedByExt);
            configuration.addSingletonInstances(List.of(mockExtension, extDefinition));
            configuration.setProgressListener(null);

            var context = contextManager.create(configuration);
            contextMap.put(id, context);

            config.getOpenExtensions().values().forEach(extConfig -> {
                var childContexts = createChildrenContext(contextManager, context, extConfig, false);
                contextMap.putAll(childContexts);
            });

            config.getSealedExtensions().values().forEach(extConfig -> {
                var childContexts = createChildrenContext(contextManager, context, extConfig, true);
                contextMap.putAll(childContexts);
            });

            return contextMap;
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

        //private final static Map<Class<?>, Emc4jTest> testClassAnnotationsCache = new java.util.concurrent.ConcurrentHashMap<>();
        private final static Map<Class<?>, BootConfig> testClassAnnotationsCache = new java.util.concurrent.ConcurrentHashMap<>();

//        public static Emc4jTest getOld(Class<?> testClass) {
//            return testClassAnnotationsCache.computeIfAbsent(testClass, cls ->
//                cls.getAnnotation(Emc4jTest.class)
//            );
//        }

        public static BootConfig get(Class<?> testClass) {
            return testClassAnnotationsCache.computeIfAbsent(testClass, cls -> {
                MergedAnnotation<Emc4jTest> merged = MergedAnnotations.from(cls, SearchStrategy.INHERITED_ANNOTATIONS).get(Emc4jTest.class);
                var bootConfig = AnnotationMapper.map(merged);
                bootConfig = BootConfigMerger.merge(bootConfig);
                return bootConfig;
            });
        }
    }

    static class EmTestContextFactory implements EmContextFactory {

        final BootConfig annotation;
        ContextManagerImpl contextManager;

        public EmTestContextFactory(BootConfig annotation) {
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

    class Helper {

        /**
         * Find classes in configured context
         * Ugly, but it will do for now
         * @param annotation the configuration annotation
         * @param layerClass the class to search for
         * @return the context UUID if found, null otherwise
         */
        static UUID findContextForClass(ContextConfig annotation, Class<?> layerClass) {

            for (var cls:annotation.getLocalClasses()) {
                if (cls.equals(layerClass)) {
                    return annotation.getUuid();
                }
            }

            for (var cls:annotation.getExportedClasses()) {
                if (cls.equals(layerClass)) {
                    return annotation.getUuid();
                }
            }

            for (var extAnnotation:annotation.getOpenExtensions().values()) {
                var found = findContextForClass(extAnnotation, layerClass);
                if (found != null) {
                    return found;
                }
            }

            for (var extAnnotation:annotation.getSealedExtensions().values()) {
                var found = findContextForClass(extAnnotation, layerClass);
                if (found != null) {
                    return found;
                }
            }

            return null;
        }
    }


}
