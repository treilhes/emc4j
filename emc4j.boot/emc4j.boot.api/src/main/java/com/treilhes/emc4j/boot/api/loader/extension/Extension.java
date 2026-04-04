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
package com.treilhes.emc4j.boot.api.loader.extension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.layer.Layer;

/**
 * Represents an extension module in the EMC4J boot system.<br>
 * Some rules about extensions <br>
 * <ul>
 *   <li>Only one main extension in the jar</li>
 *   <li>No extensions in dependencies excepted merged extensions (see: {@link #getMergedExtensions()}</li>
 *   <li>The extended extension maven dependency must have a provided scope or else emc4j will try to reload the module</li>
 * </ul>
 * Provides identification, context/layer initialization and finalization, and ordering for extensions.
 *
 * @author Pascal Treilhes
 */
public sealed interface Extension permits OpenExtension, SealedExtension, RootExtension {

    /**
     * The UUID for the boot extension.
     */
    public static final UUID BOOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /**
     * The UUID for the root extension.
     */
    public static final UUID ROOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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

    /**
     * Returns the set of extension UUIDs that this extension merges with.
     * Merging allows an extension to combine its functionality with other extensions, effectively treating them as a single unit.
     * Merging is useful for extensions that want to integrate the behavior of other extensions without creating a strict parent-child relationship.
     * Merging is transitive, meaning that if extension A merges with B, and B merges with C, then A effectively merges with C as well.
     * At the end of the loading process, all merged extensions are treated as a single extension with combined functionality and resources.
     * Merging is different from extending, as it does not imply a hierarchical relationship but rather a functional combination.
     * Merged extensions share the same context and layer, and their resources are combined during loading.
     * Only one extension in the merged set can be the root extension, and it will be used as the main entry point for the merged functionality.
     * If two or more extensions remain unmerged in the same layer an exception will occur.
     * Extensions that merge with this extension have to be provided using maven dependencies.
     * @return the set of merged extension UUIDs
     */
    public default Set<UUID> getMergedExtensions() {
        return Set.of();
    }

}
