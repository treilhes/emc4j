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
package com.treilhes.emc4j.boot.maven.client.impl;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import com.treilhes.emc4j.boot.api.context.annotation.Singleton;
import com.treilhes.emc4j.boot.api.maven.Repository;
import com.treilhes.emc4j.boot.api.maven.RepositoryManager;
import com.treilhes.emc4j.boot.maven.api.RepositoryConnectionCheck;

/**
 * Implementation of a repository connection checker that verifies if at least one repository is reachable.
 *
 * @inheritDoc
 */
@Singleton
public class RepositoryConnectionCheckImpl implements RepositoryConnectionCheck {

    private static final Logger log = LoggerFactory.getLogger(RepositoryConnectionCheckImpl.class);

    private final RepositoryManager repositoryManager;
    private final Optional<ApplicationStartup> startup;

    public RepositoryConnectionCheckImpl(RepositoryManager repositoryManager, Optional<ApplicationStartup> startup) {
        this.repositoryManager = repositoryManager;
        this.startup = startup;
    }

    @Override
    public boolean connectionOk() {
        var step = startup.map(s -> s.start("maven.repository.online.ugly.test"));
        try {
            return repositoryManager.repositories().stream().anyMatch(RepositoryConnectionCheckImpl::hasInternetConnection);
        } finally {
            step.ifPresent(StartupStep::end);
        }
    }

    private static boolean hasInternetConnection(Repository repo) {
        try {
            var url = new URI(repo.getUrl()).toURL(); // or https://example.com
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(2000); // 2 seconds
            connection.setReadTimeout(2000);
            var responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (Exception e) {
            log.warn("No connection to repository {}: {}", repo.getUrl(), e.getMessage(), e);
            return false;
        }
    }

}
