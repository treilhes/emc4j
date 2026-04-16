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
package com.treilhes.emc4j.boot.api.aop.internal;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.util.Assert;

import com.treilhes.emc4j.boot.api.aop.AopContext;

public class AopComponentProvider {

    private final AopContext<?> aopContext;
    private boolean considerNested;
    private BeanDefinitionRegistry registry;

    public AopComponentProvider(AopContext<?> aopContext, BeanDefinitionRegistry registry) {

        Assert.notNull(aopContext, "AopContext must not be null");
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        this.aopContext = aopContext;
        this.registry = registry;

    }

    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        boolean isContextCandidate = aopContext.isCandidateComponent(beanDefinition);
        boolean isTopLevelType = !beanDefinition.getMetadata().hasEnclosingClass();
        boolean isConsiderNested = isConsiderNested();
        return isContextCandidate && (isTopLevelType || isConsiderNested);
    }

    /**
     * Customizes the detection and triggers annotation detection on them.
     */
    public Map<String, AnnotatedBeanDefinition> findCandidateComponents() {

        Map<String, AnnotatedBeanDefinition> candidates = new HashMap<>();

        for (String candidateName : registry.getBeanDefinitionNames()) {
            BeanDefinition candidate = registry.getBeanDefinition(candidateName);

            if (candidate instanceof AnnotatedBeanDefinition annotatedCandidate
                    && isCandidateComponent(annotatedCandidate)) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations(annotatedCandidate);
                candidates.put(candidateName, annotatedCandidate);
            }

        }

        return candidates;
    }

    @NonNull
    protected BeanDefinitionRegistry getRegistry() {
        return registry;
    }

    public boolean isConsiderNested() {
        return considerNested;
    }

    /**
     * Controls whether nested inner-class definitions
     * should be considered for automatic discovery. This defaults to
     * {@literal false}.
     *
     * @param considerNested
     */
    public void setConsiderNested(boolean considerNested) {
        this.considerNested = considerNested;
    }

}