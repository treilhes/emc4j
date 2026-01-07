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
 * Represents an extension module in the EMC4J boot system.<br>
 * Some rules about extensions <br>
 * <ul>
 *   <li>Only one extension in the jar</li>
 *   <li>No extensions in dependencies</li>
 *   <li>The extended extension maven dependency must have a provided scope or else emc4j will try to reload the module</li>
 * </ul>
 * Provides identification, context/layer initialization and finalization, and ordering for extensions.
 *
 * @author Pascal Treilhes
 */
public sealed interface Extension permits OpenExtension, SealedExtension, RootExtension {

    /**
     * Logger for internal extension operations.
     */
    static final class PrivateLogger {
        private final static Logger logger = LoggerFactory.getLogger(Extension.class);
        private PrivateLogger() {
        }
    }

    /**
     * The UUID for the boot extension.
     */
    public final static UUID BOOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /**
     * The UUID for the root extension.
     */
    public final static UUID ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    /**
     * The UUID for the manager application extension.
     */
    public final static UUID MANAGER_APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * Returns the unique identifier for this extension.
     *
     * @return the extension UUID
     */
    UUID getId();

    /**
     * Returns the parent extension's unique identifier.
     *
     * @return the parent extension UUID
     */
    UUID getParentId();

    /**
     * Returns the list of classes that define the local context for this extension.
     * @return list of local context classes
     */
    List<Class<?>> localContextClasses();

    /**
     * Initializes the module for this extension, adding required module reads.
     *
     * @param layer the layer to initialize
     */
    public default void initializeModule(Layer layer) {
        var module = this.getClass().getModule();
        PrivateLogger.logger.info("Add read to spring.core for {}", module.getName());
        com.treilhes.emc4j.spring.core.patch.PatchLink.addRead(module);
        com.treilhes.emc4j.hibernate.core.patch.PatchLink.addRead(module);
    }

    /**
     * Initializes the context for this extension.
     *
     * @param context the context to initialize
     * @throws UnsupportedOperationException if not implemented
     */
    //FIXME this method isn't called yet
    public default void initializeContext(EmContext context) {
        throw new UnsupportedOperationException("Never called yet");
    }

    /**
     * Finalizes the context for this extension.
     *
     * @param context the context to finalize
     * @throws UnsupportedOperationException if not implemented
     */
    //FIXME this method isn't called yet
    public default void finalizeContext(EmContext context) {
        throw new UnsupportedOperationException("Never called yet");
    }

    /**
     * Finalizes the layer for this extension.
     *
     * @param layer the layer to finalize
     * @throws UnsupportedOperationException if not implemented
     */
    //FIXME this method isn't called yet
    public default void finalizeLayer(Layer layer) {
        throw new UnsupportedOperationException("Never called yet");
    }

    /**
     * Returns the order value for this extension. Lower values have higher priority.
     *
     * @return the order value
     */
    public default int getOrder() {
        return 0;
    }
//    InputStream getLicense();
//    InputStream getDescription();
//    InputStream getLoadingImage();
//    InputStream getIcon();
//    InputStream getIconX2();
}
