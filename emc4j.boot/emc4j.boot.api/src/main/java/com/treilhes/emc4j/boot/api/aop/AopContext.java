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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.context.EmContext;

/**
 * Abstract context for AOP operations, providing marker, annotation, and metadata management.
 * <p>
 * This class defines the contract for AOP context implementations, including metadata loading,
 * candidate component detection, factory bean class resolution, target creation, and exclusion annotation handling.
 * </p>
 *
 * @param <M>    the marker interface type
 * @param <A>    the context annotation type
 * @param <META> the metadata type extending {@link AopMetadata}
 */
public abstract class AopContext<M, A extends Annotation, META extends AopMetadata<A, M>> {

    /**
     * The marker interface class for AOP context.
     */
    private final Class<M> markerClass;
    /**
     * The annotation class used for context.
     */
    private final Class<A> contexAnnotationClass;

    /**
     * Constructs an AopContext with the given marker and annotation classes.
     *
     * @param markerClass           the marker interface class
     * @param contexAnnotationClass the annotation class for context
     */
    public AopContext(Class<M> markerClass, Class<A> contexAnnotationClass) {
        super();
        this.markerClass = markerClass;
        this.contexAnnotationClass = contexAnnotationClass;
    }

    /**
     * Returns the marker interface class.
     *
     * @return the marker class
     */
    public Class<M> getMarkerClass() {
        return markerClass;
    }

    /**
     * Returns the annotation class used for context.
     *
     * @return the annotation class
     */
    public Class<A> getContexAnnotationClass() {
        return contexAnnotationClass;
    }

    /**
     * Determines if the given bean definition is a candidate component for AOP.
     *
     * @param beanDefinition the bean definition to check
     * @return true if candidate, false otherwise
     */
    public abstract boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition);

    /**
     * Returns the factory bean class for this context.
     *
     * @return the factory bean class
     */
    public abstract Class<? extends AopFactoryBean<M, META>> factoryBeanClass();

    /**
     * Loads metadata for the given class.
     *
     * @param clazz the class to load metadata for
     * @return the loaded metadata
     */
    public abstract META loadMetadata(Class<?> clazz);

    /**
     * Creates the target object for the given context and metadata.
     *
     * @param context  the EmContext
     * @param metadata the metadata
     * @return the target object
     */
    public abstract M createTarget(EmContext context, META metadata);

    /**
     * Returns the exclusion annotation class for this context.
     *
     * @param <EX> the exclusion annotation type
     * @return the exclusion annotation class
     */
    public abstract <EX extends Annotation> Class<EX> getExclusionAnnotation();

    /**
     * Instantiates an object of the given class, using the context if it is a Spring component.
     *
     * @param context the EmContext
     * @param clazz   the class to instantiate
     * @param <T>     the type of the object
     * @return the instantiated object
     * @throws RuntimeException if instantiation fails
     */
    protected <T> T instanciate(EmContext context, Class<T> clazz) {
        var isComponent = AnnotationUtils.findAnnotation(clazz, Component.class) != null;
        try {
            if (isComponent) {
                return context.getBean(clazz);
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate class: " + clazz, e);
        }
    }

}

