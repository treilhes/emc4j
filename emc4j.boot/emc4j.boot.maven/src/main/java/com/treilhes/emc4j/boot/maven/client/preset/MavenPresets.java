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
package com.treilhes.emc4j.boot.maven.client.preset;

import java.util.Arrays;
import java.util.List;

import com.treilhes.emc4j.boot.api.maven.Repository;
import com.treilhes.emc4j.boot.api.maven.Repository.Content;
import com.treilhes.emc4j.boot.maven.client.type.Maven;
import com.treilhes.emc4j.boot.maven.client.type.Nexus;

/**
 * Preset Maven repositories
 * @deprecated Preset repositories are deprecated and will be removed in future releases.
 * Users should define their own default repositories explicitly in configuration file.
 * @author Pascal Treilhes
 */
@Deprecated(forRemoval = true, since = "2025-11-23")
public class MavenPresets {

    public static final String MAVEN = "Maven Central";
    public static final String SONATYPE_RELEASES = "Sonatype releases";
    public static final String SONATYPE_SNAPSHOTS = "Sonatype snaphotss";
    public static final String LOCAL = "Local";

    private static final List<Repository> presetRepositories = Arrays.asList(
            Repository.builder().id(MAVEN).type(Maven.class)
                    .url("https://repo1.maven.org/maven2/")
                    .build(),
            Repository.builder().id(SONATYPE_SNAPSHOTS).type(Nexus.class)
                    .url("https://oss.sonatype.org/content/repositories/snapshots")
                    .contentType(Content.SNAPSHOT).build(),
            Repository.builder().id(SONATYPE_RELEASES).type(Nexus.class)
                    .url("https://oss.sonatype.org/content/repositories/releases")
                    .contentType(Content.RELEASE)
                    .build());

    public static List<Repository> getPresetRepositories() {
        return presetRepositories;
    }

}
