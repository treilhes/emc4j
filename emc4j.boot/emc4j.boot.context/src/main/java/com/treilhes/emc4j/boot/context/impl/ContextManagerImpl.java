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
package com.treilhes.emc4j.boot.context.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.ContextCustomizer;
import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.EmcReadyEvent;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationConfiguration;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstancePrototype;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstanceSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationPrototype;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.DeportedSingleton;
import com.treilhes.emc4j.boot.api.context.beans.ExtensionDefinition;
import com.treilhes.emc4j.boot.api.loader.ExtensionContextConfigClasses;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.boot.context.internal.ContextHolder;

@Component
public class ContextManagerImpl implements ContextManager {

    private static final Logger logger = LoggerFactory.getLogger(ContextManagerImpl.class);
    private static final ContextHolder NULL_CONTEXT = new ContextHolder(null, null);

    private final Map<UUID, ContextHolder> uuidToContexts;
    private final Map<ModuleLayer, EmContext> layerToContexts;

    private final EmContext bootContext;
    private final EmContextFactory contextFactory;

    public ContextManagerImpl(
            EmContext bootContext,
            EmContextFactory contextFactory) {
        super();
        this.bootContext = bootContext;
        this.contextFactory = contextFactory;
        this.uuidToContexts = new HashMap<>();
        this.layerToContexts = new HashMap<>();

        registerBootContext();
    }

    private void registerBootContext() {
        if (bootContext != null) {
            var emContext = bootContext;
            var uuid = emContext.getUuid();
            var moduleLayer = this.getClass().getModule().getLayer();
            uuidToContexts.put(uuid, new ContextHolder(null, emContext));
            layerToContexts.put(moduleLayer, emContext);
        } else {
            logger.warn("Boot context is null, cannot register it in ContextManager");
        }
    }

    @Override
    public EmContext getBootContext() {
        return bootContext;
    }

    @Override
    public EmContext get(UUID contextId) {
        return uuidToContexts.getOrDefault(contextId, NULL_CONTEXT).getContext();
    }

    @Override
    public EmContext get(ModuleLayer moduleLayer) {
        return layerToContexts.get(moduleLayer);
    }

    @Override
    public boolean exists(UUID contextId) {
        return uuidToContexts.containsKey(contextId);
    }

    @Override
    public EmContext create(ContextConfiguration configuration) {

        final var extension = configuration.getExtension();
        final var parentContext = configuration.getParentContext();
        final var parentContextId = parentContext != null ? parentContext.getUuid() : null;
        final var layer = configuration.getLayer();
        final var singletonInstances = configuration.getSingletonInstances();
        final var progressListener = configuration.getProgressListener();

        final var uuid = configuration.getId();
        final ClassLoader loader;
        final ModuleLayer moduleLayer;

        if (layer != null) {
            if (layer.getId() == null || !layer.getId().equals(uuid)) {
                throw new IllegalArgumentException("Layer id must be the same than context id !");
            }
            loader = layer.getLoader();
            moduleLayer = layer.getModuleLayer();
        } else {
            loader = null;
            moduleLayer = null;
        }

        var triageExecutor = new ClassTriageExecutor();
        var triage = triageExecutor.execute(configuration);
        var contextClasses = triage.getContextClasses();
        var deportedClasses = triage.getDeportedClasses();

        var startup = new BufferingApplicationStartup(10000);
        var step = startup.start("Boot Context " + uuid);

        step.tag("classes", String.valueOf(contextClasses.size()));
        step.tag("deportedClasses", String.valueOf(triage.getDeportedClasses().size()));
        step.tag("singletonInstances", String.valueOf(singletonInstances.size()));
        if (layer != null) {
            step.tag("modules", layer.allModules().toString());
        }

        var parentHolder = uuidToContexts.get(parentContextId);
        var parent = parentHolder != null ? parentHolder.getContext() : null;

        if (parentContext != parent) {
            throw new IllegalStateException("Mismatched parent context !");
        }

        var springContext = parent != null ? parent : bootContext;

        EmContext context = contextFactory.create(springContext, uuid, loader, contextClasses, deportedClasses,
                singletonInstances, WebApplicationType.NONE);

        context.setApplicationStartup(startup);

        var holder = new ContextHolder(extension, context);
        holder.inherit(parentHolder);

        uuidToContexts.put(uuid, holder);

        if (moduleLayer != null) {
            layerToContexts.put(moduleLayer, context);
        }

        logger.info("Loading context {} with parent {} using {} classes", uuid, parentContextId, contextClasses.size());

        if (logger.isDebugEnabled()) {
            contextClasses.stream().sorted(Comparator.comparing(Class::getName)).forEach(c -> logger.debug("Loaded {}", c));
            deportedClasses.stream().sorted(Comparator.comparing(Class::getName)).forEach(c -> logger.debug("Deported {}", c));
        }

        if (progressListener != null) {
            context.addProgressListener(progressListener);
        }

        for (var cls : context.getRegisteredClasses()) {
            if (ContextCustomizer.class.isAssignableFrom(cls)) {
                logger.info("Cutomizing class {}", cls);
                try {
                    ContextCustomizer customizer = (ContextCustomizer) cls.getDeclaredConstructor().newInstance();
                    customizer.customize(contextClasses, context);
                } catch (Exception e) {
                    logger.error("Error customizing class {}", cls, e);
                }
            }

        }

        extension.initializeContext(context, holder.getShutdownHooks());

        context.refresh();
        context.start();

        step.end();

        if (context.isRunning()) {
            context.publishEvent(new EmcReadyEvent(context));
        }

        logger.info("Context {} has started successfully (active: {}, running: {}, beans: {})", context.getId(),
                context.isActive(), context.isRunning(), context.getBeanDefinitionCount());

        if (logger.isDebugEnabled()) {
            Arrays.stream(context.getBeanDefinitionNames()).sorted().forEach(c -> logger.debug("Bean {}", c));
        }

        return context;
    }

    @Override
    public EmContext createInstance(UUID contextId) {

        final var uuid = UUID.randomUUID();
        final var parentContext = get(contextId);
        final var loader = parentContext.getClassLoader();

        var deportedClasses = parentContext.getDeportedClasses();
        var processorClasses = parentContext.getRegisteredClasses().stream()
            .filter(c -> BeanPostProcessor.class.isAssignableFrom(c) || BeanFactoryPostProcessor.class.isAssignableFrom(c))
            .toList();

        // framework classes offer default features
        var frameworkClasses = BeanFactoryUtils.beansOfTypeIncludingAncestors(parentContext, ExtensionContextConfigClasses.class)
            .values().stream()
            .map(ExtensionContextConfigClasses::classes)
            .flatMap(List::stream)
            .toList();

        // there isn't any extension to initialize the context, so we need to create one
        var extension = new SealedExtension() {

            @Override
            public List<Class<?>> localContextClasses() {
                return List.of();
            }

            @Override
            public UUID getParentId() {
                return parentContext.getUuid();
            }

            @Override
            public UUID getId() {
                return uuid;
            }
        };

        var definition = new ExtensionDefinition(extension, Set.of());

        List<Class<?>> contextClasses = new ArrayList<Class<?>>();
        contextClasses.addAll(processorClasses);
        contextClasses.addAll(deportedClasses);
        contextClasses.addAll(frameworkClasses);
        contextClasses = contextClasses.stream().distinct().toList();

        EmContext context = contextFactory.create(parentContext, uuid, loader, contextClasses, List.of(),
                List.of(), WebApplicationType.NONE);

        context.registerBean(SealedExtension.class, () -> extension);
        context.registerBean(ExtensionDefinition.class, () -> definition);

        context.refresh();
        context.start();

        var parentHolder = uuidToContexts.get(parentContext.getUuid());
        var holder = new ContextHolder(extension, context);
        holder.inherit(parentHolder);

        if (context.isRunning()) {
            context.publishEvent(new EmcReadyEvent(context));
            uuidToContexts.put(uuid, holder);
        }

        logger.info("Instance context {} has started successfully (active: {}, running: {}, beans: {})", context.getId(),
                context.isActive(), context.isRunning(), context.getBeanDefinitionCount());

        if (logger.isDebugEnabled()) {
            Arrays.stream(context.getBeanDefinitionNames()).sorted().forEach(c -> logger.debug("Bean {}", c));
        }

        return context;
    }

    @Override
    public void clear() {
        uuidToContexts.keySet().forEach(this::close);
        uuidToContexts.clear();
        layerToContexts.clear();
    }

    @Override
    public synchronized void close(UUID id) {
        ContextHolder holder = uuidToContexts.remove(id);

        if (holder == null) {
            return;
        }

        EmContext ctx = holder.getContext();

        var layer = layerToContexts.entrySet().stream().filter(c -> c.getValue().getUuid().equals(id)).findFirst();
        if (layer.isPresent()) {
            layerToContexts.remove(layer.get().getKey());
        }

        if (ctx != null) {
            logger.info("Execute closing hooks for context {}", ctx.getId());
            holder.executeInheritedHooks();
            holder.executeHooks();
            if (holder.getExtension() != null) {
                holder.getExtension().finalizeContext(ctx);
            }
            logger.info("Close context {}", ctx.getId());
            ctx.close();
        }

    }

    static class ClassTriage {

        public final Set<Class<?>> contextClasses = new HashSet<>();
        public final Set<Class<?>> deportedClasses = new HashSet<>();

        public Set<Class<?>> getContextClasses() {
            return contextClasses;
        }
        public Set<Class<?>> getDeportedClasses() {
            return deportedClasses;
        }
        public void setContextClasses(Set<Class<?>> contextClasses) {
            this.contextClasses.addAll(contextClasses);
        }
        public void setDeportedClasses(Set<Class<?>> deportedClasses) {
            this.deportedClasses.addAll(deportedClasses);
        }

    }

    /**
     * Responsible for partitioning classes into context and deported sets during context creation.
     * <p>
     * The ClassTriageExecutor analyzes the provided {@link ContextConfiguration} and determines which classes
     * should be registered in the current context and which should be deported to child contexts. Deportable classes
     * are identified by specific annotations (such as {@link ApplicationSingleton}, {@link ApplicationPrototype}, etc.).
     * <p>
     * The executor handles special logic for sealed extensions and root contexts:
     * <ul>
     *   <li>Sealed extensions load deported classes locally from their parent context.</li>
     *   <li>Root contexts deport local deportable classes to child contexts.</li>
     * </ul>
     * <p>
     * The result is a {@code ClassTriage} object containing the partitioned sets of context and deported classes.
     */
    static class ClassTriageExecutor {

        private static class NoAnnotation implements Annotation {
            @Override
            public Class<? extends Annotation> annotationType() {
                return NoAnnotation.class;
            }
        }
        private static final NoAnnotation NO_ANNOTATION = new NoAnnotation();

        //@formatter:off
        private static final Set<Class<? extends Annotation>> deportableAnnotations = Set.of(
                ApplicationConfiguration.class,
                ApplicationSingleton.class,
                ApplicationPrototype.class,
                ApplicationInstanceSingleton.class,
                ApplicationInstancePrototype.class,
                DeportedSingleton.class);
        //@formatter:on


        //@formatter:off
        private static final Set<Class<? extends Annotation>> deportableInstanceAnnotations = Set.of(
                ApplicationInstanceSingleton.class,
                ApplicationInstancePrototype.class);
        //@formatter:on

        public ClassTriage executeOld(ContextConfiguration configuration) {
            var triage = new ClassTriage();
            var parentContext = configuration.getParentContext();
            var parentContextId = parentContext != null ? parentContext.getUuid() : null;
            var isSealed = configuration.isSealed();

            var parentDeportedClasses = parentContext != null ? parentContext.getDeportedClasses() : Set.<Class<?>>of();

            final var partionedByDeportable = configuration.getClasses().stream()
                    .collect(Collectors.partitioningBy(this::isDeportableClass));
            var localClasses = partionedByDeportable.get(Boolean.FALSE);
            var localDeportedClasses = partionedByDeportable.get(Boolean.TRUE);

            var childrenClassMap = configuration.getChildrenClasses().stream()
                    .collect(Collectors.partitioningBy(this::isDeportableClass));
            var childrenExportedClasses = childrenClassMap.getOrDefault(Boolean.FALSE, List.of());
            var childrenDeportedClasses = childrenClassMap.getOrDefault(Boolean.TRUE, List.of());

            var effectiveLocalClasses = new HashSet<Class<?>>();
            var effectiveDeportedClasses = new HashSet<Class<?>>();

            // add local non-deportable classes
            effectiveLocalClasses.addAll(localClasses);
            // add classes from children that aren't deportable to child contexts
            effectiveLocalClasses.addAll(childrenExportedClasses);

            if (parentContext == null) {
                //do nothing, let sealed children know about deported classes
            } else if (isSealed ) {
                // if the extension is sealed it musn't deport classes but load them locally
                // except for deportableInstanceAnnotations classes that are only for the instance context and should be deported even in sealed extensions
                var parentInstanceMapClasses = parentDeportedClasses.stream()
                        .collect(Collectors.partitioningBy(this::isDeportableInstanceClass));
                var parentLocalDeportedClasses = parentInstanceMapClasses.getOrDefault(Boolean.FALSE, List.of());
                var parentInstanceDeportedClasses = parentInstanceMapClasses.getOrDefault(Boolean.TRUE, List.of());

                var childrenInstanceMapClasses = childrenDeportedClasses.stream()
                        .collect(Collectors.partitioningBy(this::isDeportableInstanceClass));
                var childrenLocalDeportedClasses = childrenInstanceMapClasses.getOrDefault(Boolean.FALSE, List.of());
                var childrenInstanceDeportedClasses = childrenInstanceMapClasses.getOrDefault(Boolean.TRUE, List.of());

                effectiveLocalClasses.addAll(parentLocalDeportedClasses);
                effectiveLocalClasses.addAll(childrenLocalDeportedClasses);

                childrenDeportedClasses = new ArrayList<>(); // deported classes are handled, so clear them
                childrenDeportedClasses.addAll(parentInstanceDeportedClasses);
                childrenDeportedClasses.addAll(childrenInstanceDeportedClasses);
            }

            if (!Extension.BOOT_ID.equals(parentContextId)) {
                effectiveLocalClasses.addAll(localDeportedClasses);
            } else {
                // root extension is the only one to deport local classes
                effectiveDeportedClasses.addAll(localDeportedClasses);
            }

            if (childrenDeportedClasses != null) {
                effectiveDeportedClasses.addAll(childrenDeportedClasses);
            }

            triage.setContextClasses(effectiveLocalClasses);
            triage.setDeportedClasses(effectiveDeportedClasses);

            return triage;
        }

        public ClassTriage execute(ContextConfiguration configuration) {
            var triage = new ClassTriage();
            var parentContext = configuration.getParentContext();
            var parentContextId = parentContext != null ? parentContext.getUuid() : null;
            var isSealed = configuration.isSealed();

            var parentClasses = parentContext != null ? parentContext.getDeportedClasses() : Set.<Class<?>>of();
            var childrenClasses = configuration.getChildrenClasses();
            var localClasses = configuration.getClasses();

            var annotationMap = Stream.of(parentClasses, childrenClasses, localClasses).flatMap(Set::stream).collect(
                    Collectors.groupingBy(c -> this.findDeportableAnnotation(c).orElse(NO_ANNOTATION)));

            var effectiveLocalClasses = new HashSet<Class<?>>();
            var effectiveDeportedClasses = new HashSet<Class<?>>();

            if (parentContext == null) { // boot context
                // let children know about deported classes
                for (var entry : annotationMap.entrySet()) {
                    var annotation = entry.getKey();
                    var classes = entry.getValue();
                    switch (annotation) {
                        case NoAnnotation _ -> effectiveLocalClasses.addAll(classes);
                        case ApplicationConfiguration _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationPrototype _ -> effectiveDeportedClasses.addAll(classes);
                        case DeportedSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationInstanceSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationInstancePrototype _ -> effectiveDeportedClasses.addAll(classes);
                        default -> {
                            // no default possible
                        }
                    }
                }
            } else if (isSealed ) { // application context
                // if the extension is sealed it musn't deport classes but load them locally
                // except for instance classes that are only for the instance context and should be deported even in sealed extensions
                for (var entry : annotationMap.entrySet()) {
                    var annotation = entry.getKey();
                    var classes = entry.getValue();
                    switch (annotation) {
                        case NoAnnotation _ -> effectiveLocalClasses.addAll(classes);
                        case ApplicationConfiguration _ -> effectiveLocalClasses.addAll(classes);
                        case ApplicationSingleton _ -> effectiveLocalClasses.addAll(classes);
                        case ApplicationPrototype _ -> effectiveLocalClasses.addAll(classes);
                        case DeportedSingleton _ -> {
                            effectiveLocalClasses.addAll(classes);
                            effectiveDeportedClasses.addAll(classes);
                        }
                        case ApplicationInstanceSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationInstancePrototype _ -> effectiveDeportedClasses.addAll(classes);
                        default -> {
                            // no default possible
                        }
                    }
                }
            } else { // extension context
                for (var entry : annotationMap.entrySet()) {
                    var annotation = entry.getKey();
                    var classes = entry.getValue();
                    switch (annotation) {
                        case NoAnnotation _ -> effectiveLocalClasses.addAll(classes);
                        case ApplicationConfiguration _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationPrototype _ -> effectiveDeportedClasses.addAll(classes);
                        case DeportedSingleton _ -> {
                            effectiveLocalClasses.addAll(classes);
                            effectiveDeportedClasses.addAll(classes);
                        }
                        case ApplicationInstanceSingleton _ -> effectiveDeportedClasses.addAll(classes);
                        case ApplicationInstancePrototype _ -> effectiveDeportedClasses.addAll(classes);
                        default -> {
                            // no default possible
                        }
                    }
                }
            }

            triage.setContextClasses(effectiveLocalClasses);
            triage.setDeportedClasses(effectiveDeportedClasses);

            return triage;
        }

        /**
         * Determines if the specified class is deportable.
         * <p>
         * A deportable class is one that is annotated with any of the following annotations:
         * {@link ApplicationConfiguration}, {@link ApplicationSingleton}, {@link ApplicationPrototype},
         * {@link ApplicationInstanceSingleton}, {@link ApplicationInstancePrototype}, or {@link DeportedSingleton}.
         * Deportable classes are not registered in the current context but are deported to a child context for loading.
         *
         * @param cls the class to check
         * @return {@code true} if the class is annotated with any deportable annotation, {@code false} otherwise
         */
        private boolean isDeportableClass(Class<?> cls) {
            return deportableAnnotations.stream().anyMatch(a -> cls.getDeclaredAnnotationsByType(a).length > 0);
        }

        private boolean isDeportableInstanceClass(Class<?> cls) {
            return deportableInstanceAnnotations.stream().anyMatch(a -> cls.getDeclaredAnnotationsByType(a).length > 0);
        }

        private Optional<Annotation> findDeportableAnnotation(Class<?> cls) {
            return deportableAnnotations.stream()
                    .map(cls::getDeclaredAnnotationsByType)
                    .filter(l -> l.length > 0)
                    .map(l -> (Annotation)l[0])
                    .findFirst();
        }
    }
}