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
package com.treilhes.emc4j.boot.registry.internal.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.treilhes.emc4j.boot.api.layer.InvalidLayerException;
import com.treilhes.emc4j.boot.api.layer.Layer;
import com.treilhes.emc4j.boot.api.layer.ModuleLayerManager;
import com.treilhes.emc4j.boot.api.maven.Artifact;
import com.treilhes.emc4j.boot.api.maven.RepositoryClient;
import com.treilhes.emc4j.boot.api.maven.RepositoryClient.VersionType;
import com.treilhes.emc4j.boot.api.maven.UniqueArtifact;
import com.treilhes.emc4j.boot.api.registry.RegistryConfig;
import com.treilhes.emc4j.boot.api.registry.RegistryException;
import com.treilhes.emc4j.boot.registry.internal.mapper.RegistryModelMappers;
import com.treilhes.emc4j.boot.registry.internal.model.LoadState;
import com.treilhes.emc4j.boot.registry.internal.model.RegistryEntity;
import com.treilhes.emc4j.boot.registry.internal.model.RegistrySourceEntity;
import com.treilhes.emc4j.boot.registry.internal.util.BinaryCache;
import com.treilhes.emc4j.registry.mapper.Mapper;
import com.treilhes.emc4j.registry.model.Dependency;
import com.treilhes.emc4j.registry.model.Emc;
import com.treilhes.emc4j.registry.model.Registry;

import jakarta.validation.Valid;

@Service
public class RegistryUpdateServiceImpl implements RegistryUpdateService {

    private final static Logger logger = LoggerFactory.getLogger(RegistryUpdateServiceImpl.class);

    /** The maven client. */
    private final RepositoryClient mavenClient;

    /** The module layer manager. */
    private final ModuleLayerManager moduleLayerManager;

    private final RegistryConfig config;

    private final RegistryModelMappers mappers;

    private final BinaryCache cache;

    /**
     * Instantiates a new registry manager impl.
     *
     * @param mavenClient        the maven client
     * @param moduleLayerManager the module layer manager
     */
    public RegistryUpdateServiceImpl(RepositoryClient mavenClient, ModuleLayerManager moduleLayerManager,
            RegistryConfig config, RegistryModelMappers mappers, BinaryCache cache) {
        super();
        this.config = config;
        this.mavenClient = mavenClient;
        this.moduleLayerManager = moduleLayerManager;
        this.mappers = mappers;
        this.cache = cache;
    }

    @Override
    public RegistryEntity loadLatest(@Valid RegistrySourceEntity src) {

        Registry registry = null;
        RegistryEntity registryEntity = null;

        try {
            registry = src.getLocalFolder() == null ? loadFromRemoteArtifact(src) : loadFromFolder(src);

            registryEntity = mappers.map(registry);
            registryEntity.setLoadState(LoadState.SUCCESS);

        } catch (Exception e) { // catch all exceptions
            logger.error("Loading registry failed ! ", e);

            registry = new Registry();
            
            var version = src.getLocalFolder() == null ? src.getVersion() : "LOCAL";
            
            var dependency = new Dependency();
            dependency.setGroupId(src.getGroupId());
            dependency.setArtifactId(src.getArtifactId());
            dependency.setVersion(version);

            registry.setDependency(dependency);

            registryEntity = mappers.map(registry);
            registryEntity.addMessage(e.getMessage());
            registryEntity.setLoadState(LoadState.FAILURE);
        }

        final var finalRegistryEntity = registryEntity;
        registry.getRegistries().forEach(r -> {

            var coordinates = r.getDependency();
            var nestedSource = mappers.map(coordinates);
            var subRegistry = loadLatest(nestedSource);

            if (subRegistry.getApplications() != null) {
                subRegistry.getApplications().forEach(finalRegistryEntity::addApplication);
            }
            if (subRegistry.getPlugins() != null) {
                subRegistry.getPlugins().forEach(finalRegistryEntity::addPlugin);
            }
            if (subRegistry.getRepositories() != null) {
                subRegistry.getRepositories().forEach(finalRegistryEntity::addRepository);
            }
            if (subRegistry.getLoadState() == LoadState.FAILURE) {
                finalRegistryEntity.addMessage("Nested registry loading failed: " + subRegistry.getMessages());
                subRegistry.setLoadState(LoadState.PARTIAL);
            }
        });

        return registryEntity;
    }

    private Registry loadFromRemoteArtifact(RegistrySourceEntity src) throws RegistryLoadingException {

        var artifact = Artifact.builder().groupId(src.getGroupId()).artifactId(src.getArtifactId()).build();

        logger.info("Loading artifact registry {}", artifact);

        var scope = config.isSnapshotsAllowed() ? VersionType.RELEASE_SNAPHOT : VersionType.RELEASE;

        final UniqueArtifact uniqueArtifact;
        if (src.getVersion() != null && !src.getVersion().isBlank()) {
            uniqueArtifact = UniqueArtifact.builder().artifact(artifact).version(src.getVersion()).build();
        } else {
            uniqueArtifact = mavenClient.getLatestVersion(artifact, scope).orElseThrow(
                    () -> new RegistryException(String.format("Artifact not found %s scope: %s", artifact, scope)));
        }

        var resolved = mavenClient.resolveWithDependencies(uniqueArtifact)
                .orElseThrow(() -> new RegistryException(String.format("Artifact not resolved %s", uniqueArtifact)));

        var layer = createLayer(resolved.toPaths());
        try {
            return loadRegistryLayer(layer)
                    .orElseThrow(() -> new RegistryException(String.format("Layer not loaded %s", layer)));

        } catch (Exception e) { // catch all exceptions
            throw new RegistryLoadingException(
                    String.format("Loading registry from artifact %s failed !", uniqueArtifact), e);
        }

    }

    private Registry loadFromFolder(RegistrySourceEntity src) throws RegistryLoadingException {

        var folder = src.getLocalFolder();

        logger.info("Loading registry from folder {}", folder.getAbsolutePath());

        Registry registry = null;

        for (String format : Emc.REGISTRY_FILE_FORMATS) {
            try (var is = new FileInputStream(new File(folder, Emc.registryFilename(format)))) {
                registry = Mapper.get(format).from(is);
            } catch (IOException e) {
                logger.warn("Loading registry failed (format: {}) ! ", format, e);
            }

            if (registry != null) {
                break;
            }
        }

        if (registry == null) {
            throw new RegistryLoadingException(
                    String.format("Registry not found in folder %s", folder.getAbsolutePath()));
        }
        
        if (registry.getDependency() == null) {
            // the registry dependency may be missing as it is filled by the maven registry plugi
            // As it is a local registry, we can fill it with the folder name as version
            var dependency = new Dependency();
            dependency.setGroupId(src.getGroupId());
            dependency.setArtifactId(src.getArtifactId());
            dependency.setVersion(folder.getPath());
            registry.setDependency(dependency);
        }

        Function<String, InputStream> resourceToInputStream = resource -> {
            try {
                return new FileInputStream(new File(folder, resource));
            } catch (IOException e) {
                logger.error("Loading resource {} failed ! ", resource, e);
                return null;
            }
        };

        cacheBinaries(registry, resourceToInputStream);

        return registry;
    }

    private void cacheBinaries(Registry registry, Function<String, InputStream> resourceToInputStream) {
        cacheRegistryBinaries(registry, resourceToInputStream);
        cacheApplicationBinaries(registry, resourceToInputStream);
        cachePluginsBinaries(registry, resourceToInputStream);
    }

    private void cacheRegistryBinaries(Registry registry, Function<String, InputStream> resourceToInputStream) {
        if (registry == null) {
            return;
        }
        if (registry.getDescription() == null) {
            return;
        }
        cacheResource(registry.getUuid(), "image", registry.getDescription().getImage(), resourceToInputStream);
        cacheI18nResource(registry.getUuid(), "i18n", registry.getDescription().getI18n(), resourceToInputStream);
    }

    private void cachePluginsBinaries(Registry registry, Function<String, InputStream> resourceToInputStream) {
        for (var plugin : registry.getPlugins()) {
            cacheResource(plugin.getUuid(), "image", plugin.getDescription().getImage(), resourceToInputStream);
            cacheI18nResource(plugin.getUuid(), "i18n", plugin.getDescription().getI18n(), resourceToInputStream);
        }
    }

    private void cacheApplicationBinaries(Registry registry, Function<String, InputStream> resourceToInputStream) {
        for (var application : registry.getApplications()) {
            cacheResource(application.getUuid(), "splash", application.getSplash(), resourceToInputStream);
            cacheResource(application.getUuid(), "image", application.getDescription().getImage(),
                    resourceToInputStream);
            cacheI18nResource(application.getUuid(), "i18n", application.getDescription().getI18n(),
                    resourceToInputStream);
        }
    }

    private void cacheI18nResource(UUID uuid, String key, List<String> i18n, Function<String, InputStream> layer) {
        if (i18n == null) {
            return;
        }

        for (var resource : i18n) {
            var keySuffix = extractI18nSuffix(resource);
            cacheResource(uuid, key + keySuffix, resource, layer);
        }

    }

    private String extractI18nSuffix(String resource) {
        var keySuffix = "";
        var path = Path.of(resource);
        var filename = path.getFileName().toString();

        var dotIndex = filename.lastIndexOf('.') != -1 ? filename.lastIndexOf('.') : filename.length();

        if (filename.contains("_")) {
            keySuffix = filename.substring(filename.indexOf('_'), dotIndex);
        }

        return keySuffix;
    }

    private void cacheResource(UUID id, String key, String resource,
            Function<String, InputStream> resourceToInputStream) {
        if (resource == null) {
            return;
        }

        try (var is = resourceToInputStream.apply(resource)) {
            cache.add(id, key, is);
        } catch (IOException e) {
            logger.error("Loading {} failed ! ", key, e);
        }
    }

    private Layer createLayer(List<Path> a) {
        try {
            return moduleLayerManager.create(a, null);
        } catch (IOException e) {
            logger.error("Layer creation failed ! ", e);
        } catch (InvalidLayerException e) {
            logger.error("Layer validation failed ! ", e);
        }
        return null;
    }

    private Optional<Registry> loadRegistryLayer(Layer layer) {
        Objects.requireNonNull(layer);

        try {
            Registry registry = null;

            for (String format : Emc.REGISTRY_FILE_FORMATS) {
                var is = layer.getResourceAsStream(Emc.registryResourcePath(format));

                if (is == null) {
                    continue;
                }

                registry = Mapper.get(format).from(is);

                if (registry != null) {
                    break;
                }
            }

            cacheBinaries(registry, layer::getResourceAsStream);

            moduleLayerManager.remove(layer);

            return Optional.ofNullable(registry);
        } catch (IOException e) {
            logger.error("Loading registry failed !", e);
        }
        return Optional.empty();
    }
}
