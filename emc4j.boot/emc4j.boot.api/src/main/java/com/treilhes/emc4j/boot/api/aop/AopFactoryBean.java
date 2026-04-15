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

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.beans.factory.FactoryBean}
 * interface to create bean class factories
 *
 * @param <M>    the marker interface class
 * @param <D> the metadata class
 */
public abstract class AopFactoryBean<M, D extends AopMetadata<?, M>>
        implements InitializingBean, FactoryBean, BeanClassLoaderAware,
        BeanFactoryAware, ApplicationContextAware {

    private final AopContext<M, ?, D> aopContext;
    private final Class<?> beanClass;
    private final AopMetadata<?, ?> beanMetadata;

    private Function<AopContext<M, ?, D>, AopFactory> factorySupplier = null;

    private ClassLoader classLoader;
    private BeanFactory beanFactory;
    private boolean lazyInit = false;
    private String beanName = null;

    private Lazy<?> beanProxy;
    private ApplicationContext context;

    /**
     * Creates a new {@link AopFactoryBean} for the given bean class
     *
     * @param beanClass must not be {@literal null}.
     * @param aopContext the AOP context
     */
    protected AopFactoryBean(Class<?> beanClass, AopContext<M, ?, D> aopContext) {
        Assert.notNull(beanClass, "Bean class must not be null");
        this.aopContext = aopContext;
        this.beanClass = beanClass;
        this.beanMetadata = aopContext.loadMetadata(beanClass);
    }

    /**
     * Creates the {@link AopFactory} to be used to create the bean class proxy.
     *
     * @return the bean class factory
     */
    protected AopFactory createBeanFactory() {
        var beanClassFactory = factorySupplier != null ? factorySupplier.apply(aopContext) : new AopFactory(aopContext);
        beanClassFactory.setBeanClassLoader(classLoader);
        beanClassFactory.setBeanFactory(beanFactory);
        beanClassFactory.setApplicationContext(context);
        beanClassFactory.setBeanMetadata(beanMetadata);
        beanClassFactory.setBeanName(beanName);
        return beanClassFactory;
    }

    /**
     * Configures whether to initialize the bean class proxy lazily. This defaults
     * to {@literal false}.
     *
     * @param lazy whether to initialize the bean class proxy lazily. This defaults
     *             to {@literal false}.
     */
    public void setLazyInit(boolean lazy) {
        this.lazyInit = lazy;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    @NonNull
    public Object getObject() {
        return this.beanProxy.get();
    }

    @Override
    @NonNull
    public Class<?> getObjectType() {
        return beanClass;
    }

    @Override
    public boolean isSingleton() {
        return ConfigurableBeanFactory.SCOPE_SINGLETON.equals(beanMetadata.getScope());
    }

    @Override
    public void afterPropertiesSet() {

        final var factory = createBeanFactory();
        beanProxy = Lazy.of(() -> factory.getProxy(beanClass));

        if (!lazyInit && isSingleton()) {
            beanProxy.get();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public Function<AopContext<M, ?, D>, AopFactory> getFactorySupplier() {
        return factorySupplier;
    }

    public void setFactorySupplier(Function<AopContext<M, ?, D>, AopFactory> factorySupplier) {
        this.factorySupplier = factorySupplier;
    }

}