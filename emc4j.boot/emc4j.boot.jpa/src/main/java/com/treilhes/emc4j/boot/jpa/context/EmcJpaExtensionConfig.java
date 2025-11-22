/*
 * Copyright (c) 2021, 2025, Pascal Treilhes and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Pascal Treilhes nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.treilhes.emc4j.boot.jpa.context;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.annotation.ConditionalOnLocalBeanAnnotation;
import com.treilhes.emc4j.boot.api.context.annotation.LocalContextOnly;
import com.treilhes.emc4j.boot.api.jpa.ResolvablePersistenceManagedTypes;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;

@Configuration
@Import({
    PersistenceAnnotationBeanPostProcessor.class,
})
@EnableTransactionManagement(proxyTargetClass = true)
public class EmcJpaExtensionConfig {

    @Autowired
    private EmContext context;

    public EmcJpaExtensionConfig(EmContext context) {
        super();
        this.context = context;
    }

    /**
     * This method is used to create the entity manager factory for the local
     * context Mainly here to propagate the classloader
     *
     * @param dataSource       the data source
     * @param jpaVendorAdapter the jpa vendor adapter
     * @param ext              the extensions
     * @param ctx              the context
     * @return
     */
    @Bean("entityManagerFactory")
    // @Bean("otherEntityManagerFactory")
    //@ConditionalOnMissingBean(EntityManagerFactory.class)
    @ConditionalOnLocalBeanAnnotation(Entity.class)
    LocalContainerEntityManagerFactoryBean localEntityManagerFactory(DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter, EmContext ctx,
            ResolvablePersistenceManagedTypes persistenceManagedTypes) {

        var em = new LocalContainerEntityManagerFactoryBean();
        //em.setJtaDataSource(dataSource);
        em.setDataSource(dataSource);
        em.setJpaVendorAdapter(jpaVendorAdapter);
        em.setManagedTypes(persistenceManagedTypes);
        em.setPersistenceUnitName(ctx.getId());
        em.setPersistenceUnitPostProcessors(new EmcPersistenceRulesCheck());
        em.setJpaProperties(hibernateProperties());
        em.setBeanFactory(ctx.getBeanFactory());
        em.setBeanClassLoader(persistenceManagedTypes.getClassLoader());

        em.getJpaPropertyMap().put(AvailableSettings.CLASSLOADERS, List.of(persistenceManagedTypes.getClassLoader()));
        return em;
    }

    /**
     * This method is used to create the transaction manager for the local context
     * Mainly here to propagate the classloader
     */
    @Bean(name = "transactionManager")
    @ConditionalOnLocalBeanAnnotation(Entity.class)
    PlatformTransactionManager localTransactionManager(EntityManagerFactory factory, DataSource dataSource, @LocalContextOnly Extension extension) {
        var tm = new JpaTransactionManager();
        //tm.setTransactionManagerName(extension.getId().toString());
        tm.setEntityManagerFactory(factory);
        tm.setDataSource(dataSource);
        tm.setNestedTransactionAllowed(true);
        return tm;
    }

    /**
     * This method is used to create the repository factory for the local context
     * Mainly here to propagate the classloader
     *
     * @param mngr the entity manager
     * @return the repository factory
     */
    @ConditionalOnBean(name = "entityManagerFactory")
    @Bean
    RepositoryFactorySupport factoryBean(EntityManager mngr) {
        return new JpaRepositoryFactory(mngr);
    }

    /**
     * Hibernate configuration properties. Do not use the spring.jpa.hibernate.*
     * prefix as it is not supported by hibernate.
     *
     * @return the hibernate properties
     */
    final Properties hibernateProperties() {
        final var hibernateProperties = new Properties();

        hibernateProperties.setProperty("hibernate.show_sql", "true");
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "update");
        // hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");

//          hibernateProperties.setProperty("spring.jpa.open-in-view", "false");

//          hibernateProperties.setProperty("spring.jpa.hibernate.naming.physical-strategy",
//                  "com.treilhes.emc4j.boot.jpa.ModuleAwarePhysicalNamingStrategyStandardImpl");
//          hibernateProperties.setProperty("spring.jpa.hibernate.naming.implicit-strategy",
//                  "com.treilhes.emc4j.boot.jpa.ModuleAwareImplicitNamingStrategyLegacyJpaImpl");

        hibernateProperties.setProperty("hibernate.physical_naming_strategy",
                "com.treilhes.emc4j.boot.jpa.naming.ModuleAwarePhysicalNamingStrategyStandardImpl");
        hibernateProperties.setProperty("hibernate.implicit_naming_strategy",
                "com.treilhes.emc4j.boot.jpa.naming.ModuleAwareImplicitNamingStrategyLegacyJpaImpl");
        hibernateProperties.setProperty("hibernate.id.db_structure_naming_strategy",
                "com.treilhes.emc4j.boot.jpa.naming.ModuleAwareImplicitDatabaseObjectNamingStrategyImpl");
        //
        //
        // hibernateProperties.setProperty("spring.datasource.driverClassName",
        // "org.h2.Driver");
        // hibernateProperties.setProperty("spring.datasource.url",
        // "jdbc:h2:file:./emc4j-h2-db");
        // hibernateProperties.setProperty("spring.datasource.username", "sa");
        // hibernateProperties.setProperty("spring.datasource.password", "");

        return hibernateProperties;
    }

    /**
     * This class is used to prevent the usage of some annotations in JPA entities.
     * The annotations that are forbidden are: - {@link Table} The annotation
     * attributes that are forbidden are: - sequence_name for
     * {@link GenericGenerator}
     *
     * The list isn't exhaustive and can be extended in the future.
     *
     * The forbidden annotations are forbidden because they allow customization of
     * the underlying database schema. Extensions share the same schema and the
     * schema is managed by the application to prevent name collisions.
     *
     */
    class EmcPersistenceRulesCheck implements PersistenceUnitPostProcessor {

        /**
         * This method is called by the JPA provider to validate the JPA entities
         * detected in the classpath.
         *
         * @param pui the persistence unit info
         * @throws EmcJpaForbiddenException if a forbidden JPA annotation or
         *                                      attribute is found
         */
        @Override
        public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
            for (String clsName : pui.getManagedClassNames()) {
                var cls = context.getRegisteredClass(clsName);

                // prevent table name
                var tableAnnotationExists = cls.getAnnotationsByType(Table.class).length > 0;
                if (tableAnnotationExists) {
                    throw new EmcJpaForbiddenException(
                            Table.class.getName() + " is a forbidden JPA annotation");
                }

                // prevent sequence_name
                var nameParamExist = Stream.of(cls.getDeclaredFields())
                        .flatMap(f -> Stream.of(f.getAnnotationsByType(GenericGenerator.class)))
                        .flatMap(g -> Stream.of(g.parameters())).anyMatch(p -> "sequence_name".equals(p.name()));

                if (nameParamExist) {
                    throw new EmcJpaForbiddenException(GenericGenerator.class.getName()
                            + " sequence_name parameter is a forbidden JPA annotation");
                }

            }
        }

    }

    /**
     * This exception is thrown when a forbidden JPA annotation or attribute is
     * found in the detected JPA entities.
     */
    class EmcJpaForbiddenException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public EmcJpaForbiddenException(String message, Throwable cause) {
            super(message, cause);
        }

        public EmcJpaForbiddenException(String message) {
            super(message);
        }
    }
}