/*
 * Copyright (c) 2021, 2024, Pascal Treilhes and/or its affiliates.
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
package com.treilhes.emc4j.boot.maven.client.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.treilhes.emc4j.boot.api.maven.Artifact;
import com.treilhes.emc4j.boot.api.maven.Classifier;
import com.treilhes.emc4j.boot.api.maven.MavenConfig;
import com.treilhes.emc4j.boot.api.maven.MavenConfig.Redirect;
import com.treilhes.emc4j.boot.api.maven.Repository;
import com.treilhes.emc4j.boot.api.maven.RepositoryClient;
import com.treilhes.emc4j.boot.api.maven.RepositoryType;
import com.treilhes.emc4j.boot.api.maven.ResolvedArtifact;
import com.treilhes.emc4j.boot.api.maven.UniqueArtifact;
import com.treilhes.emc4j.boot.maven.api.RepositoryMapper;


public class RedirectedRepositoryClientImpl implements RepositoryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedirectedRepositoryClientImpl.class);

    private final RepositoryClient client;
    private final MavenConfig config;
    private final RepositoryMapper mappers;

    public RedirectedRepositoryClientImpl(MavenConfig config, RepositoryMapper mappers,
            RepositoryClient client) {
        super();
        this.config = config;
        this.mappers = mappers;
        this.client = client;
    }

    private Optional<Redirect> findMatch(Artifact artifact) {
        var redirect = config.getRedirect().stream()
                .filter(r -> Objects.equals(artifact.getGroupId(), r.groupId()))
                .filter(r -> {
                    if (r.useRegex()) {
                        return artifact.getArtifactId().matches(r.artifactId());
                    } else {
                        return Objects.equals(artifact.getArtifactId(), r.artifactId());
                    }
                })
                .map(r -> {
                    if (r.useRegex()) {
                        var groupId = artifact.getGroupId();
                        var artifactId = artifact.getArtifactId();
                        var pattern = Pattern.compile(r.artifactId());

                        var matcher = pattern.matcher(artifactId);
                        if (matcher.matches()) {
                            // Replace placeholders with captured groups
                            var path = matcher.replaceAll(r.path());

                            if (!Files.exists(Path.of(path))) {
                                LOGGER.warn("Redirection exist for artifact {}:{} but lead to a non existing path {}", artifact.getGroupId(), artifact.getArtifactId(),
                                        Path.of(r.path()).toAbsolutePath().normalize());
                                return null;
                            }
                            return new Redirect(groupId, artifactId, path, true);
                        }
                        return null;
                    } else {
                        return r;
                    }
                })
                .filter(Objects::nonNull)
                .findAny();

        redirect.ifPresent(r -> LOGGER.info("Redirecting artifact {}:{} to path {}", artifact.getGroupId(), artifact.getArtifactId(),
              Path.of(r.path()).toAbsolutePath().normalize()));

        return redirect;
    }

    @Override
    public RepositoryClient repositories(List<Repository> repositories) {
        return new RedirectedRepositoryClientImpl(config, mappers,
                client.repositories(repositories));
    }

    @Override
    public RepositoryClient localPath(File path) {
        return new RedirectedRepositoryClientImpl(config, mappers,
                client.localPath(path));
    }

    @Override
    public RepositoryClient localOnly() {
        return new RedirectedRepositoryClientImpl(config, mappers, client.localOnly());
    }

    @Override
    public List<UniqueArtifact> getAvailableVersions(Artifact artifact) {
        return findMatch(artifact).map(mappers::map).map(ResolvedArtifact::getUniqueArtifact).map(List::of)
                .orElseGet(() -> client.getAvailableVersions(artifact));
    }

    @Override
    public List<UniqueArtifact> getAvailableVersions(Artifact artifact, VersionType scope) {
        return findMatch(artifact).map(mappers::map).map(ResolvedArtifact::getUniqueArtifact).map(List::of)
                .orElseGet(() -> client.getAvailableVersions(artifact, scope));
    }

    @Override
    public Optional<UniqueArtifact> getLatestVersion(Artifact artifact) {
        return findMatch(artifact).map(mappers::map).map(ResolvedArtifact::getUniqueArtifact)
                .or(() -> client.getLatestVersion(artifact));
    }

    @Override
    public Optional<UniqueArtifact> getLatestVersion(Artifact artifact, VersionType scope) {
        return findMatch(artifact).map(mappers::map).map(ResolvedArtifact::getUniqueArtifact)
                .or(() -> client.getLatestVersion(artifact, scope));
    }

    @Override
    public Optional<ResolvedArtifact> resolve(UniqueArtifact artifact) {
        return findMatch(artifact.getArtifact()).map(mappers::map).or(() -> client.resolve(artifact));
    }

    @Override
    public Optional<ResolvedArtifact> resolveWithDependencies(UniqueArtifact artifact) {

        Optional<ResolvedArtifact> redirect = findMatch(artifact.getArtifact()).map(mappers::map);

        Optional<ResolvedArtifact> resolved = client.resolveWithDependencies(artifact);

        if (resolved.isEmpty()) { // not found in repo
            return redirect;
        }

        // redirect dependencies
        var redirectedDependencies = new ArrayList<ResolvedArtifact>();

        resolved.ifPresent(ra -> ra.getDependencies().forEach(d -> {
            Optional<ResolvedArtifact> raRedirect = findMatch(d.getUniqueArtifact().getArtifact()).map(mappers::map);
            redirectedDependencies.add(raRedirect.orElse(d));
        }));

        var source = redirect.orElse(resolved.get());

        var target = Optional.of(ResolvedArtifact.builder().artifact(source.getUniqueArtifact())
                .path(source.getPath()).dependencies(redirectedDependencies).build());

        return target;
    }

    @Override
    public Optional<ResolvedArtifact> resolveWithDependencies(ResolvedArtifact artifact) {
        return this.resolveWithDependencies(artifact.getUniqueArtifact());
    }

    @Override
    public Map<Classifier, Optional<ResolvedArtifact>> resolve(UniqueArtifact artifact, List<Classifier> classifiers) {
        Optional<ResolvedArtifact> redirected = findMatch(artifact.getArtifact()).map(mappers::map);
        if (redirected.isPresent()) {
            return Map.of(Classifier.DEFAULT, redirected);
        } else {
            return client.resolve(artifact, classifiers);
        }
    }

    @Override
    public Map<Classifier, Optional<ResolvedArtifact>> resolve(ResolvedArtifact artifact,
            List<Classifier> classifiers) {
        return this.resolve(artifact.getUniqueArtifact(), classifiers);
    }

    @Override
    public boolean deployToLocalRepository(ResolvedArtifact artifact) {
        return client.deployToLocalRepository(artifact);
    }

    @Override
    public List<Repository> repositories() {
        return client.repositories();
    }

    @Override
    public String validate(Repository repository) {
        return client.validate(repository);
    }

    @Override
    public Set<Artifact> search(String query) {
        // maybe redirection must contribute to search here, wait and see
        return client.search(query);
    }

    @Override
    public Set<Class<? extends RepositoryType>> repositoryTypes() {
        return client.repositoryTypes();
    }

}
