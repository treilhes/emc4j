package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterEachCallback;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;

import com.treilhes.emc4j.boot.api.layer.Layer;
import com.treilhes.emc4j.boot.api.layer.ModuleLayerManager;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.RootExtension;
import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.boot.context.impl.ContextManagerImpl;
import com.treilhes.emc4j.boot.context.impl.EmContextImpl;
import com.treilhes.emc4j.boot.loader.internal.context.ContextBootstraper;
import com.treilhes.emc4j.boot.loader.internal.context.ContextBootstraper.ServiceLoader;
import com.treilhes.emc4j.boot.loader.model.LoadableContent;

public class Emc4jExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final static Logger logger = LoggerFactory.getLogger(Emc4jExtension.class);
    private final static Namespace EMC4J = create("com.treilhes.emc4j");

    // This constructor is invoked by JUnit Jupiter via reflection or ServiceLoader
    @SuppressWarnings("unused")
    public Emc4jExtension() {
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

        @Override
        protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
            return Emc4jContextLoader.class;
        }

        @Override
        protected String[] getProperties(Class<?> testClass) {
            return MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS).get(Emc4jTest.class)
                    .getValue("properties", String[].class).orElse(null);
        }

    }

    public static class Emc4jContextLoader extends AbstractContextLoader {

        protected static final ThreadLocal<com.treilhes.emc4j.boot.api.context.EmContext> testContextHolder = new ThreadLocal<>();

        private Class<?> testClass;

        private Boolean loadDefaultScopes;

        public Emc4jContextLoader() {
            super();
        }

        @Override
        protected String[] generateDefaultLocations(Class<?> clazz) {
            this.testClass = clazz;

            this.loadDefaultScopes = MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
                    .get(Emc4jTest.class).getValue("loadDefaultScopes", Boolean.class).orElse(null);

            return super.generateDefaultLocations(clazz);
        }

        @Override
        public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
            EmContextImpl.applicationScope.clear();

            var contextId = SealedExtension.ROOT_ID;
            var parent = Mockito.mock(ExtensionContext.class);
            var loader = Mockito.mock(ServiceLoader.class);
            var extensionDefinition = Mockito.mock(LoadableContent.class);
            var extension = Mockito.mock(RootExtension.class);
            var layer = Mockito.mock(Layer.class);
            var layerManager = Mockito.mock(ModuleLayerManager.class);
            // we want the context manager to return the same context everytime
            var contextManager = new ContextManagerImpl(null);
            var bootstraper = new ContextBootstraper(layerManager, contextManager);

            when(layer.getId()).thenReturn(contextId);
            when(layer.getModuleLayer()).thenReturn(ModuleLayer.boot());
            when(layerManager.get(any())).thenReturn(layer);
            when(loader.loadService(layer, Extension.class)).thenReturn(Set.of(extension));

            when(extension.getId()).thenReturn(contextId);

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


            //classes.addAll(new JpaBootClasses().classes());
            //@formatter:on

            when(extension.localContextClasses()).thenReturn(classes);
            //@formatter:on
            //var ctx = bootstraper.create(parent, extensionDefinition, List.of(contextManager), null, loader);

            //var ctx = BootContext.create(classes, new String[0]);

            var ctx = new EmContextImpl(contextId);
            ctx.register(classes.toArray(new Class[0]));
            ctx.refresh();

            if (loadDefaultScopes) {
                // set the current scopes
                var appBean = ctx.getBean(Emc4jTest.Application1Bean.class);
                logger.info("Loaded Application Bean: {}", appBean);
                var instanceBean = ctx.getBean(Emc4jTest.Application1InstanceBean.class);
                logger.info("Loaded Application Instance Bean: {}", instanceBean);

            }

            testContextHolder.set(ctx);

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

    @TestConfiguration
    //@AutoConfigureMockMvc
    static class Emc4jTestConfig {

        @Bean
        @ConditionalOnMissingBean
        public DataSource dataSource() {
            var dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:testdb"); // Pour une base en mémoire ou file:./data/testdb pour fichier
            dataSource.setUsername("sa");
            dataSource.setPassword(""); // H2 par défaut n'a pas de mot de passe pour l'utilisateur 'sa'

            return dataSource;
        }

        @Bean
        @ConditionalOnBean(name = "servletContext")
        public JpaVendorAdapter jpaVendorAdapter() {
            var adapter = new HibernateJpaVendorAdapter();
            adapter.setGenerateDdl(true); // Générer automatiquement le schéma de base de données
            adapter.setShowSql(true); // Afficher les requêtes SQL dans la console
            adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect"); // Utilisation de H2
            return adapter;
        }

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
}
