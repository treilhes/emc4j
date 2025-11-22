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
package com.treilhes.emc4j.boot.registry.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.treilhes.emc4j.boot.api.platform.EmcPlatform;

@Component
public class BinaryCache {
	private static final Logger logger = LoggerFactory.getLogger(BinaryCache.class);
	private static final String CACHE_FOLDER_NAME = "Emc4jCache";
	private EmcPlatform platform;

	public BinaryCache(EmcPlatform platform) {
		this.platform = platform;
		ensureCacheFolderExist();
	}

	public void add(UUID id, String key, InputStream content) {
		try {
			Files.copy(content, toPath(id, key), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("Error copying binary to cache", e);
		} finally {
			try {
				content.close();
			} catch (IOException e) {
				logger.error("Error closing input stream", e);
			}
		}
	}

	public URL get(UUID id, String key) {
		try {
			var path = toPath(id, key);
			if (!Files.exists(path)) {
				return null;
			}
			return toPath(id, key).toUri().toURL();
		} catch (MalformedURLException e) {
			logger.error("Error creating URL from cache", e);
			return null;
		}
	}

	public InputStream getInputStream(UUID id, String key) {
		try {
			return Files.newInputStream(toPath(id, key));
		} catch (IOException e) {
			logger.error("Error creating input stream from cache", e);
			return null;
		}
	}

	private Path toPath(UUID id, String key) {
		return cacheFolder().toPath().resolve(id + "-" + key);
	}

	private File cacheFolder() {
		return new File(platform.rootFile(), CACHE_FOLDER_NAME);
	}

	private void ensureCacheFolderExist() {
		var cache = cacheFolder();
		if (!cache.exists()) {
			cache.mkdirs();
		}
	}
}
