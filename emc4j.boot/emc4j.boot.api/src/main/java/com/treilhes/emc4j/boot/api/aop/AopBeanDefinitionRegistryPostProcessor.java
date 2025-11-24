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
package com.treilhes.emc4j.boot.api.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

import com.treilhes.emc4j.boot.api.aop.internal.AopBeanConfiguration;
import com.treilhes.emc4j.boot.api.aop.internal.AopBeanFactoryDefinitionRegistrar;
import com.treilhes.emc4j.boot.api.aop.internal.AopBeanNameGenerator;
import com.treilhes.emc4j.boot.api.aop.internal.AopComponentProvider;
import com.treilhes.emc4j.boot.api.context.EmContext;

/**
 * BeanDefinitionRegistryPostProcessor for registering AOP beans in the Spring context.
 * <p>
 * This abstract class is responsible for scanning and registering AOP-related bean definitions
 * using a custom {@link AopComponentProvider} and {@link AopBeanFactoryDefinitionRegistrar}.
 * It works with an {@link EmContext} to retrieve registered classes and uses a custom
 * {@link AopBeanNameGenerator} for bean naming. Subclasses should provide an {@link AopContext}
 * to configure AOP behavior.
 * </p>
 * <ul>
 *   <li>Scans for candidate components using {@link AopComponentProvider}.</li>
 *   <li>Registers bean definitions for AOP beans via {@link AopBeanFactoryDefinitionRegistrar}.</li>
 *   <li>Integrates with Spring's {@link ApplicationContext} and {@link BeanDefinitionRegistry}.</li>
 * </ul>
 *
 * @author Pascal Treilhes
 * @since 1.0
 */
public abstract class AopBeanDefinitionRegistryPostProcessor
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AopBeanDefinitionRegistryPostProcessor.class);

    /**
     * The AOP context used for processing and configuration.
     */
    private final AopContext aopContext;
    /**
     * Bean name generator for Spring beans, defaults to {@link AnnotationBeanNameGenerator}.
     */
    private final BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
    /**
     * The Spring application context, set via {@link #setApplicationContext(ApplicationContext)}.
     */
    private ApplicationContext context;

    /**
     * Constructor accepting an AopContext.
     *
     * @param aopContext the AOP context to be used for processing
     */
    public AopBeanDefinitionRegistryPostProcessor(AopContext aopContext) {
        super();
        this.aopContext = aopContext;
    }


    /**
     * Scans and registers AOP bean definitions in the provided {@link BeanDefinitionRegistry}.
     * <p>
     * Uses {@link AopComponentProvider} to find candidate components and
     * {@link AopBeanFactoryDefinitionRegistrar} to register them. Only operates if the
     * {@link ApplicationContext} is an instance of {@link EmContext}.
     * </p>
     *
     * @param registry the bean definition registry
     * @throws BeansException if an error occurs during bean definition registration
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        if (context instanceof EmContext emc) {

            var registrar = new AopBeanFactoryDefinitionRegistrar(registry);

            var scanner = new AopComponentProvider(aopContext, registry);
            scanner.setConsiderNested(true);

            var candidates = scanner.findCandidateComponents();

            for (var candidate : candidates) {
                logger.debug("Candidate: {}", candidate.getBeanClassName());

                var clazz = emc.getRegisteredClass(candidate.getBeanClassName());
                var generator = new AopBeanNameGenerator(clazz.getClassLoader(), beanNameGenerator);
                var pc = new AopBeanConfiguration(clazz.getClassLoader(), aopContext, candidate);

                registrar.register(pc, generator);
            }

        }

    }

    /**
     * Sets the Spring {@link ApplicationContext} for this post processor.
     *
     * @param applicationContext the application context to set
     * @throws BeansException if context cannot be set
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
