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

import java.lang.reflect.InvocationTargetException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.treilhes.emc4j.boot.api.aop.internal.DefaultMethodInterceptor;
import com.treilhes.emc4j.boot.api.context.EmContext;

/**
 * Factory for creating AOP proxy instances and managing AOP-related metadata and context.
 * <p>
 * This class is responsible for creating proxy objects for beans, setting up interceptors,
 * and managing the Spring and EMC4J contexts required for AOP operations.
 * </p>
 *
 * @author Pascal Treilhes
 * @since 1.0
 */
public class AopFactory implements BeanClassLoaderAware, BeanFactoryAware, ApplicationContextAware {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AopFactory.class);

    /** The AOP context used for target creation. */
    private AopContext aopContext;
    /** The class loader used for proxy creation. */
    private ClassLoader classLoader;
    /** The Spring bean factory. */
    private BeanFactory beanFactory;
    /** Metadata for the bean being proxied. */
    private AopMetadata beanMetadata;
    /** The Spring application context. */
    private ApplicationContext context;

    /**
     * Creates a new {@link AopFactory} with the specified AOP context.
     *
     * @param aopContext the AOP context to use for proxy creation
     */
    public AopFactory(AopContext aopContext) {
        this.aopContext = aopContext;
        this.classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();
    }

    /**
     * Sets the class loader to be used for proxy creation.
     *
     * @param classLoader the class loader to set
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader()
                : classLoader;
    }

    /**
     * Sets the Spring bean factory.
     *
     * @param beanFactory the bean factory to set
     * @throws BeansException if the bean factory cannot be set
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Returns a proxy instance for the given interface backed by an instance
     * providing implementation logic.
     *
     * @param beanClass the class of the bean interface to proxy
     * @param <T>       the type of the bean
     * @return a proxy instance implementing the given interface
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T getProxy(Class<T> beanClass) {

        if (logger.isDebugEnabled()) {
            logger.debug("Initializing proxy target instance for {}", beanClass.getName());
        }

        Assert.notNull(beanClass, "Bean class must not be null");

        var target = aopContext.createTarget((EmContext) context, beanMetadata);

        // Create proxy
        var result = new ProxyFactory();
        result.setTarget(target);
        result.setInterfaces(beanClass);

        // TODO: check if this realy needed
        result.addAdvisor(ExposeInvocationInterceptor.ADVISOR);

        AopFactory.class.getModule().addReads(beanClass.getModule());

        result.addAdvice(new DefaultMethodInterceptor());
        result.addAdvice(new ImplementationInterceptor(target, beanClass));

        var proxy = (T) result.getProxy(beanClass.getClassLoader());

        if (logger.isDebugEnabled()) {
            logger.debug("Finished creation of proxy target instance for {}.", beanClass.getName());
        }

        return proxy;
    }

    /**
     * Method interceptor that calls methods on the target object.
     */
    static class ImplementationInterceptor implements MethodInterceptor {

        /** The target object to invoke methods on. */
        private final Object base;
        /** The bean class being proxied. */
        private Class<?> beanClass;

        /**
         * Constructs an ImplementationInterceptor for the given target and bean class.
         *
         * @param base      the target object
         * @param beanClass the bean class being proxied
         */
        public ImplementationInterceptor(Object base, Class<?> beanClass) {
            this.base = base;
            this.beanClass = beanClass;
        }

        /**
         * Intercepts method calls and delegates them to the target object.
         *
         * @param invocation the method invocation
         * @return the result of the method call
         * @throws Throwable if the method invocation fails
         */
        @Nullable
        @Override
        public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

            var method = invocation.getMethod();
            Object[] arguments = invocation.getArguments();

            try {
                return method.invoke(base, arguments);
            } catch (Exception e) {
                if (e instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) e).getTargetException();
                }
                throw e;
            }
        }
    }

    /**
     * Sets the metadata for the bean being proxied.
     *
     * @param beanMetadata the bean metadata to set
     */
    public void setBeanMetadata(AopMetadata beanMetadata) {
        this.beanMetadata = beanMetadata;
    }

    /**
     * Sets the Spring application context.
     *
     * @param applicationContext the application context to set
     * @throws BeansException if the context cannot be set
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
