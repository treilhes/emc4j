package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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

    private enum InjectType {
        EMC_INJECT,
        EMC_INJECT_MOCK,
        EMC_INJECT_SPY
    }

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

        var emcInjectFields = new HashMap<Field, InjectDefinition>();

        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(EmcInject.class)) {
                var annotation = field.getAnnotation(EmcInject.class);
                var def = loadDefinition(field.getType(), annotation);
                emcInjectFields.put(field, def);
            }
            if (field.isAnnotationPresent(EmcInjectMock.class)) {
                var annotation = field.getAnnotation(EmcInjectMock.class);
                var def = loadDefinition(field.getType(), annotation);
                emcInjectFields.put(field, def);
            }
            if (field.isAnnotationPresent(EmcInjectSpy.class)) {
                var annotation = field.getAnnotation(EmcInjectSpy.class);
                var def = loadDefinition(field.getType(), annotation);
                emcInjectFields.put(field, def);
            }
        }

        emcInjectFields.forEach((field, def) -> {
            switch (def.injectType) {
                case EMC_INJECT -> registerEmInjectBean(def, contextMap);
                case EMC_INJECT_MOCK -> registerEmInjectMockBean(def, contextMap);
                case EMC_INJECT_SPY -> registerEmInjectSpyBean(def, contextMap);
            }
        });

        emcInjectFields.forEach((field, def) -> {
            field.setAccessible(true);
            Object dependency = getBean(def, contextMap);

            try {
                field.set(testInstance, dependency);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // no-op
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Check if the parameter is supported, e.g., by type or annotation
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
        var parameter = parameterContext.getParameter();
        var type = parameterContext.getParameter().getType();

        InjectDefinition def = null;
        var emcInject = parameter.getAnnotation(EmcInject.class);
        if (emcInject != null) {
            def = loadDefinition(parameter.getType(), emcInject);
        }

        var emcSpy = parameter.getAnnotation(EmcInjectSpy.class);
        if (emcSpy != null) {
            def = loadDefinition(type, emcSpy);
        }

        var emcMock = parameter.getAnnotation(EmcInjectMock.class);
        if (emcMock != null) {
            def = loadDefinition(type, emcMock);
        }

        if (EmContext.class.isAssignableFrom(type)) {
            var contextId = def != null ? def.getContextId() : Extension.BOOT_ID;
            var context = contextMap.get(contextId);
            if (context !=null) {
                return context;
            }
        }

        if (def != null) {
            switch (def.injectType) {
                case EMC_INJECT -> registerEmInjectBean(def, contextMap);
                case EMC_INJECT_MOCK -> registerEmInjectMockBean(def, contextMap);
                case EMC_INJECT_SPY -> registerEmInjectSpyBean(def, contextMap);
            }
            return getBean(def, contextMap);
        }

        return null;
    }

    private InjectDefinition loadDefinition(Class<?> type, EmcInject emcInject) {
        var contextId = !emcInject.contextId().isBlank() ? emcInject.contextId() : Extension.BOOT_ID.toString();
        var qualifier = !emcInject.qualifier().isBlank()? emcInject.qualifier() : null;
        var create = emcInject.create();
        return new InjectDefinition(InjectType.EMC_INJECT ,type, contextId, qualifier, create);
    }

    private InjectDefinition loadDefinition(Class<?> type, EmcInjectMock emcInject) {
        var contextId = !emcInject.contextId().isBlank() ? emcInject.contextId() : Extension.BOOT_ID.toString();
        var qualifier = !emcInject.qualifier().isBlank()? emcInject.qualifier() : null;
        var create = true;
        return new InjectDefinition(InjectType.EMC_INJECT_MOCK ,type, contextId, qualifier, create);
    }

    private InjectDefinition loadDefinition(Class<?> type, EmcInjectSpy emcInject) {
        var contextId = !emcInject.contextId().isBlank() ? emcInject.contextId() : Extension.BOOT_ID.toString();
        var qualifier = !emcInject.qualifier().isBlank()? emcInject.qualifier() : null;
        var create = emcInject.create();
        return new InjectDefinition(InjectType.EMC_INJECT_SPY ,type, contextId, qualifier, create);
    }

    private void registerEmInjectBean(InjectDefinition def, Map<UUID, EmContext> contextMap) {
        var type = def.getType();
        var uuid = def.getContextId();
        var name = def.getQualifier();
        var create = def.isCreate();
        var context = contextMap.get(uuid);

        if (EmContext.class.isAssignableFrom(type)) {
            return;
        }

        if (context == null) {
            return;
        }

        if (create) {

            var customizers = new ArrayList<BeanDefinitionCustomizer>();
            if (def.getScope() != null) {
                customizers.add(bd -> bd.setScope(def.getScope().value()));
            }
            if (def.getPrimary() != null) {
                customizers.add(bd -> bd.setPrimary(true));
            }

            if (name != null) {
                if (context.containsBean(name)) {
                    context.removeBeanDefinition(name);
                }
                context.registerBean(name, type, customizers.toArray(BeanDefinitionCustomizer[]::new));
            } else {
                context.registerBean(type, customizers.toArray(BeanDefinitionCustomizer[]::new));
            }
        }
    }

    private <T> void registerEmInjectSpyBean(InjectDefinition def, Map<UUID, EmContext> contextMap) {
        var type = def.getType();
        var uuid = def.getContextId();
        var name = def.getQualifier();
        var create = def.isCreate();
        var context = contextMap.get(uuid);

        if (EmContext.class.isAssignableFrom(type)) {
            return;
        }

        if (context == null) {
            return;
        }

        var customizers = new ArrayList<BeanDefinitionCustomizer>();
        if (def.getScope() != null) {
            customizers.add(bd -> bd.setScope(def.getScope().value()));
        }
        //if (def.getPrimary() != null) {
            customizers.add(bd -> bd.setPrimary(true));
        //}

        if (create) {
            if (name != null) {
                if (context.containsBean(name)) {
                    context.removeBeanDefinition(name);
                }
                context.registerBean(name, type, customizers.toArray(BeanDefinitionCustomizer[]::new));
            } else {
                context.registerBean(type, customizers.toArray(BeanDefinitionCustomizer[]::new));
            }
        }

        Object beanInstance = getBean(def, contextMap);
        if (Mockito.mockingDetails(beanInstance).isSpy()) {
            Mockito.reset(beanInstance);
            return;
        }


        Class<T> beanType = (Class<T>)beanInstance.getClass();
        Object spyInstance = Mockito.spy(beanInstance);

        if (name != null) {
            context.removeBeanDefinition(name);
            context.registerBean(name, beanType, () -> beanType.cast(spyInstance), customizers.toArray(BeanDefinitionCustomizer[]::new));
        } else {
            context.registerBean(beanType, () -> beanType.cast(spyInstance), customizers.toArray(BeanDefinitionCustomizer[]::new));
        }
    }

    private <T> void registerEmInjectMockBean(InjectDefinition def, Map<UUID, EmContext> contextMap) {
        Class<T> type = (Class<T>) def.getType();
        var uuid = def.getContextId();
        var name = def.getQualifier();
        var create = def.isCreate();
        var context = contextMap.get(uuid);

        if (context == null) {
            return;
        }

        var customizers = new ArrayList<BeanDefinitionCustomizer>();
        if (def.getScope() != null) {
            customizers.add(bd -> bd.setScope(def.getScope().value()));
        }
        if (def.getPrimary() != null) {
            customizers.add(bd -> bd.setPrimary(true));
        }

        try {
            Object existingBean = getBean(def, contextMap);
            if (existingBean != null) {
                Mockito.reset(existingBean);
                return;
            }
        } catch (BeansException e) {
            // Bean does not exist, proceed to create a mock
        }

        if (name != null) {
            if (context.containsBean(name)) {
                context.removeBeanDefinition(name);
            }
            context.registerBean(name, type, () -> Mockito.mock(type), customizers.toArray(BeanDefinitionCustomizer[]::new));
        } else {
            context.registerBean(type, () -> Mockito.mock(type), customizers.toArray(BeanDefinitionCustomizer[]::new));
        }
    }

    private Object getBean(InjectDefinition def, Map<UUID, EmContext> contextMap) {
        var type = def.getType();
        var uuid = def.getContextId();
        var name = def.getQualifier();
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

    static class InjectDefinition {
        private InjectType injectType;
        private Scope scope;
        private Primary primary;
        private final Class<?> type;
        private final UUID contextId;
        private final String qualifier;
        private final boolean create;

        private InjectDefinition(InjectType injectType, Class<?> type, String contextId, String qualifier, boolean create) {
            this.injectType = injectType;
            this.type = type;
            this.contextId = contextId != null && ! contextId.isBlank()? UUID.fromString(contextId) : null;
            this.qualifier = qualifier != null && ! qualifier.isBlank() ? qualifier : null;
            this.create = create;
        }

        public InjectDefinition(InjectType injectType, Parameter parameter, String contextId, String qualifier, boolean create) {
            this(injectType, parameter.getType(), contextId, qualifier, create);
            this.scope = parameter.getAnnotation(Scope.class);
            this.primary = parameter.getAnnotation(Primary.class);
            initAnnotationsIfNotSet();
        }

        public InjectDefinition(InjectType injectType, Field field, String contextId, String qualifier, boolean create) {
            this(injectType, field.getType(), contextId, qualifier, create);
            this.scope = field.getAnnotation(Scope.class);
            this.primary = field.getAnnotation(Primary.class);
            initAnnotationsIfNotSet();
        }

        private void initAnnotationsIfNotSet() {
            var clsScope = type.getAnnotation(Scope.class);
            var clsPrimary = type.getAnnotation(Primary.class);

            if (scope == null && clsScope != null) {
                this.scope = clsScope;
            }
            if (primary == null && clsPrimary != null) {
                this.primary = clsPrimary;
            }
        }

        public Class<?> getType() {
            return type;
        }

        public Scope getScope() {
            return scope;
        }

        public Primary getPrimary() {
            return primary;
        }

        public UUID getContextId() {
            return contextId;
        }

        public String getQualifier() {
            return qualifier;
        }

        public boolean isCreate() {
            return create;
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
            properties.add("spring.main.lazy-initialization=true");

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

            WebApplicationType webAppType = bootConfig.getWebEnvironment() != WebEnvironment.NONE
                    ? WebApplicationType.SERVLET
                    : WebApplicationType.NONE;

            var contextMap = new HashMap<UUID, EmContext>();
            var bootContext = createBootContext(mergedConfig, classes, webAppType);
            contextMap.put(Extension.BOOT_ID, bootContext);

            var contextManager = bootContext.getBean(ContextManager.class);

            if (bootConfig.getSealedExtensions().containsKey(Extension.ROOT_ID)) {
                var rootConfig = bootConfig.getSealedExtensions().get(Extension.ROOT_ID);
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

            testContextHolder.set(contextMap);

            return bootContext;
        }

        private EmContextImpl createBootContext(MergedContextConfiguration mergedConfig, ArrayList<Class<?>> classes,
                WebApplicationType webAppType) {
            var ctx = new EmContextImpl(Extension.BOOT_ID, webAppType);
            var factory = new Emc4jTestContextFactory(this.bootConfig);
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

            ctx.addBeanFactoryPostProcessor(new ForceLazyPostProcessor());
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
            configuration.setExtension(mockExtension);
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
    static class Emc4jTestConfig {

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

        private static final Map<Class<?>, BootConfig> testClassAnnotationsCache = new java.util.concurrent.ConcurrentHashMap<>();

        private Emc4jAnnotationCache() {
            /* This utility class should not be instantiated */
        }

        public static BootConfig get(Class<?> testClass) {
            return testClassAnnotationsCache.computeIfAbsent(testClass, cls -> {
                MergedAnnotation<Emc4jTest> merged = MergedAnnotations.from(cls, SearchStrategy.INHERITED_ANNOTATIONS).get(Emc4jTest.class);
                var bootConfig = AnnotationMapper.map(merged);
                bootConfig = BootConfigMerger.merge(bootConfig);
                return bootConfig;
            });
        }
    }

    static class Emc4jTestContextFactory implements EmContextFactory {

        final BootConfig annotation;
        ContextManagerImpl contextManager;

        public Emc4jTestContextFactory(BootConfig annotation) {
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

            context.addBeanFactoryPostProcessor(new ForceLazyPostProcessor());

            context.setParent(parent);
            context.register(classes.toArray(Class<?>[]::new));
            context.deport(deportedClasses.toArray(new Class<?>[0]));

            if (singletonInstances != null) {
                singletonInstances.forEach(context::registerSingleton);
            }

            return context;
        }

    }

    /**
     * BeanFactoryPostProcessor to force all beans to be lazy initialized.
     * Mainly to allow emc4j annotation to be used before any initialization of the beans.
     */
    private static class ForceLazyPostProcessor implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            for (String name : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                bd.setLazyInit(true);
            }
        }
    }

    class Helper {
        private Helper() {
            /* This utility class should not be instantiated */
        }

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
