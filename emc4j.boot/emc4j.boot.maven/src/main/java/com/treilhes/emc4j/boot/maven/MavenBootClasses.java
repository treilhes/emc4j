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
package com.treilhes.emc4j.boot.maven;

import java.util.List;

import com.treilhes.emc4j.boot.api.loader.BootContextConfigClasses;
import com.treilhes.emc4j.boot.api.maven.MavenConfig;
import com.treilhes.emc4j.boot.maven.client.impl.MavenRepositoryClientImpl;
import com.treilhes.emc4j.boot.maven.client.impl.RedirectedRepositoryBeanPostProcessor;
import com.treilhes.emc4j.boot.maven.client.impl.RepositoryManagerImpl;
import com.treilhes.emc4j.boot.maven.client.impl.RepositoryMapperImpl;
import com.treilhes.emc4j.boot.maven.client.model.Repository;
import com.treilhes.emc4j.boot.maven.client.repository.RepositoryRepository;
import com.treilhes.emc4j.boot.maven.client.type.Maven;
import com.treilhes.emc4j.boot.maven.client.type.Nexus;

public class MavenBootClasses implements BootContextConfigClasses {

    @Override
    public List<Class<?>> classes() {
        return List.of(
                MavenConfig.class,
                MavenRepositoryClientImpl.class,
                RedirectedRepositoryBeanPostProcessor.class,
                RepositoryManagerImpl.class,
                RepositoryMapperImpl.class,

                Maven.class,
                Nexus.class,

                //model
                Repository.class,

                //repository
                RepositoryRepository.class
                );
    }

}
