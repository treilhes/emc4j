/*
 * Copyright (c) 2021, 2026, Pascal Treilhes and/or its affiliates.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.MultipleProgressListener;
import com.treilhes.emc4j.boot.api.context.beans.ExtensionDefinition;
import com.treilhes.emc4j.boot.api.layer.Layer;
import com.treilhes.emc4j.boot.api.layer.ModuleLayerManager;
import com.treilhes.emc4j.boot.api.loader.ExtensionContextConfigClasses;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.loader.extension.OpenExtension;
import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.boot.loader.model.LoadableContent;
import com.treilhes.emc4j.boot.loader.validation.ExtensionValidator;
import com.treilhes.emc4j.boot.loader.validation.ExtensionValidatorImpl;

/**
 * The Class ContextBootstraper.
 */
@Component
public class ExtensionBootstrapper {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExtensionBootstrapper.class);

    /** The context manager. */
    private final ContextManager contextManager;

    /** The layer manager. */
    private final ModuleLayerManager layerManager;

    private final ExtensionValidator extensionValidator;

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
    // @formatter:off
    public ExtensionBootstrapper(
            ModuleLayerManager layerManager,
            ContextManager contextManager,
            ExtensionValidator extensionValidator) {
        super();
        // @formatter:on
        this.contextManager = contextManager;
        this.layerManager = layerManager;
        this.extensionValidator = extensionValidator;
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

        var mainExtDefinition = loadMainExtension(loader, layer);
        var childrenExtDefinitions = loadChildExtensions(loader, content.getExtensions());
        var isSealed = SealedExtension.class.isInstance(mainExtDefinition.getExtension());

        validateExtensions(id, parentContextId, mainExtDefinition, childrenExtDefinitions);

        initializeExtensions(mainExtDefinition, childrenExtDefinitions);

        var frameworkExtensionClasses = loadFrameworkClasses(parent);

        var localClasses = loadLocalClasses(mainExtDefinition);
        var childrenExportedClasses = loadExportedClasses(childrenExtDefinitions);

        var singletons = new ArrayList<>(singletonInstances);
        singletons.add(mainExtDefinition);

        var configuration = new ContextConfiguration();
        configuration.setId(id);
        configuration.setParentContext(parent);
        configuration.setSealed(isSealed);
        configuration.setLayer(layer);
        configuration.addClasses(frameworkExtensionClasses);
        configuration.addClasses(localClasses);
        configuration.addChildrenClasses(childrenExportedClasses);
        configuration.addSingletonInstances(singletons);
        configuration.setProgressListener(progressListener);

        var context = contextManager.create(configuration);

        return context;
    }

    private void initializeExtensions(ExtensionDefinition extension, Set<ExtensionDefinition> extensions)
            throws LayerNotFoundException {
        initializeExtension(extension);
        for (var childExtension : extensions) {
            initializeExtension(childExtension);
        }
    }

    private void initializeExtension(ExtensionDefinition definition)
            throws LayerNotFoundException {

        var extension = definition.getExtension();
        var id = extension.getId();
        var layer = layerManager.get(id);

        if (layer == null) {
            throw new LayerNotFoundException(id, "Unable to find child layer for id %s");
        }

        initializeExtension(layer, extension);

        for (var childExtension : definition.getMixins()) {
            initializeExtension(layer, childExtension);
        }
    }

    private void validateExtensions(UUID id, UUID parentContextId, ExtensionDefinition definition,
            Set<ExtensionDefinition> childrenDefinitions) {

        validateExtensionDefinition(id, parentContextId, definition);

        childrenDefinitions.forEach(child -> validateExtensionDefinition(child.getExtension().getId(), id, child));
    }

    private void validateExtensionDefinition(UUID id, UUID parentId, ExtensionDefinition definition) {

        validateExtension(definition.getExtension(), id, parentId);

        definition.getMixins().forEach(mixin -> validateExtension(mixin, mixin.getId(), null));

    }

    private List<Class<?>> loadExportedClasses(Set<ExtensionDefinition> definitions) {

        var exportedClasses = new ArrayList<Class<?>>();

        for (var def : definitions) {
            if (def.getExtension() instanceof OpenExtension openExtension) {
                exportedClasses.addAll(openExtension.exportedContextClasses());
            }

            for (var mixin : def.getMixins()) {
                if (mixin instanceof OpenExtension openMixin) {
                    exportedClasses.addAll(openMixin.exportedContextClasses());
                }
            }
        }

        return exportedClasses;
    }

    private List<Class<? extends Object>> loadLocalClasses(ExtensionDefinition definition) {

        var localClasses = new ArrayList<Class<? extends Object>>();
        var ext = definition.getExtension();
        var mixins = definition.getMixins();

        localClasses.add(ext.getClass());
        localClasses.addAll(ext.localContextClasses());

        mixins.forEach(mixin -> {
            localClasses.add(mixin.getClass());
            localClasses.addAll(mixin.localContextClasses());
        });

        return localClasses;
    }

    private List<Class<?>> loadFrameworkClasses(EmContext parent) {

        if (parent == null) {
            return List.of();
        }

        return BeanFactoryUtils.beansOfTypeIncludingAncestors(parent, ExtensionContextConfigClasses.class)
                .values().stream()
                .map(ExtensionContextConfigClasses::classes)
                .flatMap(List::stream)
                .toList();
    }

    private ExtensionDefinition loadMainExtension(ServiceLoader loader, Layer layer) {
        return loadDescriptor(loader, layer);
    }

    /**
     * Load child extensions.
     *
     * @param loadableContents the extension id
     * @return the set
     * @throws LayerNotFoundException    the layer not found exception
     * @throws InvalidExtensionException the invalid extension exception
     */
    private Set<ExtensionDefinition> loadChildExtensions(ServiceLoader loader, Set<LoadableContent> loadableContents)
            throws LayerNotFoundException, InvalidExtensionException {

        var extensions = new HashSet<ExtensionDefinition>();

        for (var loadableContent : loadableContents) {
            var id = loadableContent.getId();
            var layer = layerManager.get(id);

            if (layer == null) {
                throw new LayerNotFoundException(id, "Unable to find child layer for id %s");
            }

            ExtensionDefinition descriptor = loadDescriptor(loader, layer);

            if (descriptor.getExtension() instanceof OpenExtension) {
                extensions.add(descriptor);
            }
        }

        return extensions;
    }

    private ExtensionDefinition loadDescriptor(ServiceLoader loader, Layer layer) {
        var extensions = loader.loadService(layer, Extension.class).stream().toList();

        if (extensions.isEmpty()) {
            throw new ExtensionNotFoundException(layer.getId(), "Layer %s does not contain any extension");
        }

        if (extensions.size() == 1) {
            return new ExtensionDefinition(extensions.get(0), Set.of());
        }

        // If multiple extensions are found, we look for the one matching the layer id
        var extension = extensions.stream()
                .filter(e -> e.getId().equals(layer.getId()))
                .findAny()
                .orElseThrow(() -> new ExtensionNotFoundException(layer.getId(),
                        "Multiple extensions found in layer %s but none match the layer id"));

        // If we found an extension matching the layer id, but there are multiple extensions in the layer, we must ensure consistency
        // by checking that all extensions in the layer are part of the same merge tree (i.e. they all have the same root ancestor extension)
        // After removing the merged extensions from the list, only one extension should remain, which is the one we will use as the main extension for the layer
        var mergeTree = new ArrayList<>(extensions);
        for (Extension e : extensions) {
            boolean isMerged = mergeTree.stream().anyMatch(ext -> ext.getMergedExtensions().contains(e.getId()));

            if (isMerged) {
                mergeTree.remove(e);
            }
        }

        if (mergeTree.size() != 1) {
            var msg = "Multiple extensions found in layer %s but they are not part of the same merge tree";
            throw new ExtensionNotFoundException(layer.getId(), msg);
        }

        var mixins = extensions.stream().filter(e -> e != extension).collect(Collectors.toSet());

        return new ExtensionDefinition(extension, mixins);
    }

    /**
     * Checks that the extension is valid, and that its id and parent id match the expected values.
     * @see ExtensionValidatorImpl#isValid(Extension)
     * @param extension         the extension
     * @param expectedId        the expected id
     * @param expectedParentId  the expected parent id
     * @return true, if extension is valid
     */
    private boolean validateExtension(Extension extension, UUID expectedId, UUID expectedParentId) {
        if (!extensionValidator.isValid(extension)) {
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

    private void initializeExtension(Layer layer, Extension extension) throws LayerNotFoundException {

        var module = extension.getClass().getModule();

        logger.info("Add read to spring.core for {}", module.getName());
        com.treilhes.emc4j.spring.core.patch.PatchLink.addRead(module);

        logger.info("Add read to hibernate.core for {}", module.getName());
        com.treilhes.emc4j.hibernate.core.patch.PatchLink.addRead(module);

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
