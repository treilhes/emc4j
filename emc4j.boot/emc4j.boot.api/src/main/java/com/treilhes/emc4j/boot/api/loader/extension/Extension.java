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
package com.treilhes.emc4j.boot.api.loader.extension;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.layer.Layer;

/**
 * Some rules about extensions <br>
 * - Only one extension in the jar <br>
 * - No extensions in dependencies<br>
 * - The extended component library must have a provided scope<br>
 *
 * @author ptreilhes
 *
 */
public sealed interface Extension permits OpenExtension, SealedExtension, RootExtension {

    static final class PrivateLogger {
        private final static Logger logger = LoggerFactory.getLogger(Extension.class);
        private PrivateLogger() {
        }
    }

    public final static UUID BOOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public final static UUID ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public final static UUID MANAGER_APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    UUID getId();

    UUID getParentId();

    List<Class<?>> localContextClasses();

    public default void initializeModule(Layer layer) {
        var module = this.getClass().getModule();

        PrivateLogger.logger.info("Add read to spring.core for {}", module.getName());

        com.treilhes.emc4j.spring.core.patch.PatchLink.addRead(module);
        com.treilhes.emc4j.hibernate.core.patch.PatchLink.addRead(module);
    }

    /**
     * @param context
     */
    //FIXME this method isn't called yet
    public default void initializeContext(EmContext context) {
        throw new UnsupportedOperationException("Never called yet");
    }

    /**
     * @param context
     */
  //FIXME this method isn't called yet
    public default void finalizeContext(EmContext context) {
        throw new UnsupportedOperationException("Never called yet");
    }

    /**
     * @param layer
     */
  //FIXME this method isn't called yet
    public default void finalizeLayer(Layer layer) {
        throw new UnsupportedOperationException("Never called yet");
    }

    public default int getOrder() {
        return 0;
    }
//    InputStream getLicense();
//
//    InputStream getDescription();
//
//    InputStream getLoadingImage();
//
//    InputStream getIcon();
//
//    InputStream getIconX2();
}
