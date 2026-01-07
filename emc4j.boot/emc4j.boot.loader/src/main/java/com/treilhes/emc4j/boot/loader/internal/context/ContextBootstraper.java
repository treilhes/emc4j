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
package com.treilhes.emc4j.boot.loader.internal.context;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.MultipleProgressListener;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationConfiguration;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstancePrototype;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationInstanceSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationPrototype;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.api.context.annotation.DeportedSingleton;
import com.treilhes.emc4j.boot.api.layer.Layer;
import com.treilhes.emc4j.boot.api.layer.ModuleLayerManager;
import com.treilhes.emc4j.boot.api.loader.ExtensionContextConfigClasses;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;
import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.boot.loader.extension.ExtensionValidator;
import com.treilhes.emc4j.boot.loader.model.LoadableContent;

/**
 * The Class ContextBootstraper.
 */
@Component
public class ContextBootstraper {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ContextBootstraper.class);

    //@formatter:off
    private static final Set<Class<? extends Annotation>> deportableAnnotations = Set.of(
            ApplicationConfiguration.class,
            ApplicationSingleton.class,
            ApplicationPrototype.class,
            ApplicationInstanceSingleton.class,
            ApplicationInstancePrototype.class,
            DeportedSingleton.class);
    //@formatter:on

    /** The context manager. */
    private final ContextManager contextManager;

    /** The layer manager. */
    private final ModuleLayerManager layerManager;

    private static final ServiceLoader DEFAULT_LOADER = new ServiceLoader() {
        @Override
        public <T> Set<T> loadService(Layer layer, Class<T> serviceClass) {
            return java.util.ServiceLoader.load(layer.getModuleLayer(), serviceClass).stream()
                    .map(java.util.ServiceLoader.Provider::get)
                    .filter(e -> e.getClass().getModule().getLayer().equals(layer.getModuleLayer()))
                    .collect(Collectors.toSet());
        }
    };

    /**
     * Instantiates a new context bootstraper.
     *
     * @param layerManager the layer manager
     */
    public ContextBootstraper(ModuleLayerManager layerManager, ContextManager contextManager) {
        super();
        this.contextManager = contextManager;
        this.layerManager = layerManager;
    }

    /**
     * Gets the.
     *
     * @param extension the extension
     * @return the context
     */
    public EmContext get(com.treilhes.emc4j.boot.loader.model.LoadableContent extension) {
        return contextManager.get(extension.getId());
    }

    /**
     * Gets the.
     *
     * @param extensionId the extension id
     * @return the context
     */
    public EmContext get(UUID extensionId) {
        return contextManager.get(extensionId);
    }

    /**
     * Exists.
     *
     * @param extension the extension
     * @return true, if successful
     */
    public boolean exists(com.treilhes.emc4j.boot.loader.model.LoadableContent extension) {
        return contextManager.exists(extension.getId());
    }

    /**
     * Creates the context for the given extension.
     * @param parent the parent context
     * @param extension the current extension to load
     * @param singletonInstances singletons bean to add into the context
     * @param progressListener the progress listener
     * @return the context
     * @throws InvalidExtensionException
     * @throws LayerNotFoundException
     */
    public EmContext create(EmContext parent, com.treilhes.emc4j.boot.loader.model.LoadableContent extension, List<Object> singletonInstances,
            MultipleProgressListener progressListener) throws InvalidExtensionException, LayerNotFoundException {
        return create(parent, extension, singletonInstances, progressListener, DEFAULT_LOADER);
    }

    /**
     * Creates the extension context for the given extension.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Retrieves the module layer for the given extension.</li>
     *   <li>Collects exported and deported classes from all child extensions.</li>
     *   <li>Collects local context classes for the current extension.</li>
     *   <li>Aggregates all relevant classes (exported, deported, local, and additional config classes) to be included in the context.</li>
     *   <li>Handles special logic for sealed extensions and the root extension regarding deported classes.</li>
     *   <li>Builds a {@link ContextConfiguration} with the collected classes, deported classes, singleton instances, and progress listener.</li>
     *   <li>Creates and returns the new {@link EmContext} using the {@link ContextManager}.</li>
     * </ul>
     *
     * @param parent             the parent context, or {@code null} if this is the root context
     * @param extension          the extension to load
     * @param singletonInstances the singleton beans to add into the context
     * @param progressListener   the progress listener for context creation
     * @param loader             the service loader to use for loading extension services
     * @return the created {@link EmContext}
     * @throws InvalidExtensionException if the extension or any child extension is invalid
     * @throws LayerNotFoundException    if the module layer for the extension or any child cannot be found
     */
    public EmContext create(EmContext parent, com.treilhes.emc4j.boot.loader.model.LoadableContent content, List<Object> singletonInstances,
            MultipleProgressListener progressListener, ServiceLoader loader)
            throws InvalidExtensionException, LayerNotFoundException {

        var id = content.getId();
        var parentContextId = parent == null ? null : parent.getUuid();
        var layer = layerManager.get(id);

        if (layer == null) {
            throw new LayerNotFoundException(id, "Unable to find layer for id %s");
        }

        var extension = loadMainExtension(loader, layer);
        var extensions = loadChildExtensions(loader, content.getExtensions());
        var isSealed = SealedExtension.class.isInstance(extension);

        validateExtensions(id, parentContextId, extension, extensions);

        initializeExtensions(extension, extensions);

        var frameworkExtensionClasses = loadFrameworkClasses(parent);

        var localClasses = loadLocalClasses(extension);
        var childrenExportedClasses = loadExportedClasses(extensions);


//        // get children extensions
//        Set<UUID> extensionIds = extension.getExtensions().stream().map(LoadableContent::getId).collect(Collectors.toSet());
//
//
//
//        Set<Class<?>> classes = new HashSet<>();
//        Set<Class<?>> extensionLocalClasses = new HashSet<>();
//        Set<Class<?>> childrenExportedClasses = new HashSet<>();
//        Set<Class<?>> childrenDeportedClasses = new HashSet<>();
//
//        for (UUID extensionId : extensionIds) {
//            try {
//                var map = findExportedClasses(loader, id, extensionId);
//                childrenExportedClasses.addAll(map.getOrDefault(ExportType.EXPORTED, List.of()));
//                childrenDeportedClasses.addAll(map.getOrDefault(ExportType.DEPORTED, List.of()));
//            } catch (LayerNotFoundException e) {
//                logger.error("Unable to find layer for child extension {}", extensionId, e);
//            } catch (InvalidExtensionException e) {
//                logger.error("Child extension is not valid {}", extensionId, e);
//            }
//        }
//
//        extensionLocalClasses.addAll(findLocalClasses(loader, parentContextId, layer));
//
//        classes.addAll(childrenExportedClasses);
//
//
//
//        classes.addAll(frameworkExtensionClasses);
//
//        if (parent == null) {
//            //do nothing, let sealed children know about deported classes
//        } else if (isSealed ) {
//            classes.addAll(parent.getDeportedClasses());
//            classes.addAll(childrenDeportedClasses);
//            childrenDeportedClasses.clear(); // deported classes are handled, so clear them
//        }
//
//        if (!Extension.BOOT_ID.equals(parentContextId)) {
//            classes.addAll(extensionLocalClasses);
//        } else {
//            // root extension is the only one to deport local classes
//            final var partionedByDeportable = extensionLocalClasses.stream()
//                    .collect(Collectors.partitioningBy(this::isDeportableClass));
//
//            classes.addAll(partionedByDeportable.get(Boolean.FALSE));
//            childrenDeportedClasses.addAll(partionedByDeportable.get(Boolean.TRUE));
//        }

        var configuration = new ContextConfiguration();
        configuration.setId(id);
        configuration.setParentContext(parent);
        configuration.setSealed(isSealed);
        configuration.setLayer(layer);
        //configuration.addClasses(classes);
        //configuration.addDeportedClasses(childrenDeportedClasses);

        configuration.addClasses(frameworkExtensionClasses);
        configuration.addClasses(localClasses);
        configuration.addChildrenClasses(childrenExportedClasses);

        configuration.addSingletonInstances(singletonInstances);
        configuration.setProgressListener(progressListener);

        var context = contextManager.create(configuration);

        return context;
    }

    private void initializeExtensions(Extension extension, Set<OpenExtension> extensions)
            throws LayerNotFoundException {
        initializeExtension(extension);
        for (var childExtension : extensions) {
            initializeExtension(childExtension);
        }
    }

    private void validateExtensions(UUID id, UUID parentContextId, Extension extension, Set<OpenExtension> extensions) {
        validateExtension(extension, id, parentContextId);
        extensions.forEach(child -> validateExtension(child, child.getId(), id));
    }

    private List<Class<?>> loadExportedClasses(Set<OpenExtension> extensions) {
        return extensions.stream().flatMap(e -> e.exportedContextClasses().stream()).toList();
    }

    private List<Class<? extends Object>> loadLocalClasses(Extension extension) {
        return Stream.concat(Stream.of(extension.getClass()), extension.localContextClasses().stream()).toList();
    }

    private List<Class<?>> loadFrameworkClasses(EmContext parent) {

//        return java.util.ServiceLoader.load(ExtensionContextConfigClasses.class).stream()
//                .map(Provider::get)
//                .map(ExtensionContextConfigClasses::classes)
//                .flatMap(List::stream)
//                .toList();

        if (parent == null) {
            return List.of();
        }

        return BeanFactoryUtils.beansOfTypeIncludingAncestors(parent, ExtensionContextConfigClasses.class)
                .values().stream()
                .map(ExtensionContextConfigClasses::classes)
                .flatMap(List::stream)
                .toList();
    }

    private Extension loadMainExtension(ServiceLoader loader, Layer layer) {
        return loader.loadService(layer, Extension.class).stream().findFirst()
                .orElseThrow(() -> new ExtensionNotFoundException(layer.getId(), "Layer %s does not contain any extension"));
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

    /**
     * Represents the type of class export in the extension context.
     * <p>
     * Used to distinguish between classes that are exported to child contexts and those that are deported (transferred) to child contexts for loading.
     */
    private enum ExportType {
        /**
         * Classes that are exported to the current context and are available for use in this context.
         */
        EXPORTED,
        /**
         * Classes that are deported to the current or next {@link SealedExtension}
         * context, meaning they are registered in the current context if it is a {@link SealedExtension} or are made
         * available to the next child contexts which are {@link SealedExtension} for loading.
         */
        DEPORTED
    }

    /**
     * Find exported and deported classes for the provided extension id.
     *
     * @param parentId    the parent id
     * @param extensionId the extension id
     * @return the map
     * @throws LayerNotFoundException    the layer not found exception
     * @throws InvalidExtensionException the invalid extension exception
     */
    private Map<ExportType, List<Class<?>>> findExportedClasses(Set<OpenExtension> extensions)
            throws LayerNotFoundException, InvalidExtensionException {

        return extensions.stream()
                .flatMap(e -> e.exportedContextClasses().stream())
                .collect(Collectors.groupingBy(c -> isDeportableClass(c) ? ExportType.DEPORTED : ExportType.EXPORTED));

    }

    /**
     * Load child extensions.
     *
     * @param loadableContents the extension id
     * @return the set
     * @throws LayerNotFoundException    the layer not found exception
     * @throws InvalidExtensionException the invalid extension exception
     */
    private Set<OpenExtension> loadChildExtensions(ServiceLoader loader, Set<LoadableContent> loadableContents)
            throws LayerNotFoundException, InvalidExtensionException {

        var extensions = new HashSet<OpenExtension>();

        for (var loadableContent : loadableContents) {
            var id = loadableContent.getId();
            var layer = layerManager.get(id);

            if (layer == null) {
                throw new LayerNotFoundException(id, "Unable to find child layer for id %s");
            }

            Extension extension = loader.loadService(layer, Extension.class).stream().findAny()
                    .orElseThrow(() -> new ExtensionNotFoundException(layer.getId(),
                            "Child layer %s does not contain any extension"));

            if (extension instanceof OpenExtension openExtension) {
                extensions.add(openExtension);
            }
        }

        return extensions;
    }

    /**
     * Find local classes. Classes that will be registered in the current context
     * and provided by the current extension.
     *
     * @param parentId the parent id
     * @param layer    the layer
     * @return the sets the
     * @throws LayerNotFoundException    the layer not found exception
     * @throws InvalidExtensionException the invalid extension exception
     */
    private Set<Class<?>> findLocalClasses(ServiceLoader loader, UUID parentId, Layer layer)
            throws LayerNotFoundException, InvalidExtensionException {
        try {
            return loader.loadService(layer, Extension.class).stream()
                    .peek(e -> validateExtension(e, layer.getId(), parentId))
                    .peek(e -> e.initializeModule(layer))
                    .flatMap(e -> Stream.concat(Stream.of(e.getClass()), e.localContextClasses().stream()))
                    .collect(Collectors.toSet());
        } catch (InvalidExtensionException.Unchecked e) {
            throw new InvalidExtensionException(e);
        }
    }

    /**
     * Checks that the extension is valid, and that its id and parent id match the expected values.
     * @see ExtensionValidator#isValid(Extension)
     * @param extension         the extension
     * @param expectedId        the expected id
     * @param expectedParentId  the expected parent id
     * @return true, if extension is valid
     */
    private boolean validateExtension(Extension extension, UUID expectedId, UUID expectedParentId) {
        if (!ExtensionValidator.isValid(extension)) {
            throw new InvalidExtensionException.Unchecked(extension.toString());
        }
        if (!extension.getId().equals(expectedId)) {
            var msg = "Invalid extension id expected : %s but was %s";
            msg = String.format(msg, expectedId, extension.getId());
            throw new InvalidExtensionException.Unchecked(msg);
        }
        if (expectedParentId != null && !extension.getParentId().equals(expectedParentId)) {
            var msg = "Invalid extension %s parent id expected : %s but was %s";
            msg = String.format(msg, expectedId, expectedParentId, extension.getParentId());
            throw new InvalidExtensionException.Unchecked(msg);
        }
        return true;
    }

    private void initializeExtension(Extension extension) throws LayerNotFoundException {

        var id = extension.getId();
        var layer = layerManager.get(id);

        if (layer == null) {
            throw new LayerNotFoundException(id, "Unable to find child layer for id %s");
        }

        extension.initializeModule(layer);
    }

    /**
     * Clear.
     */
    public void clear() {
        contextManager.clear();
    }

    /**
     * Close.
     *
     * @param extension the extension
     */
    public void close(com.treilhes.emc4j.boot.loader.model.LoadableContent extension) {
        contextManager.close(extension.getId());
    }

    @FunctionalInterface
    public interface ServiceLoader {
        <T> Set<T> loadService(Layer layer, Class<T> serviceClass);
    }
}
