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
package com.treilhes.emc4j.boot.context.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.treilhes.emc4j.boot.api.context.ContextConfiguration;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;

@ExtendWith(MockitoExtension.class)
class ContextManagerImplTriageExecutorTest {

    ContextManagerImpl.ClassTriageExecutor triageExecutor;

    @Mock
    EmContext mockParentContext;

    @BeforeEach
    void init() {
        triageExecutor = new ContextManagerImpl.ClassTriageExecutor();
    }


    //@Test
    void local_classes_must_be_separated_by_deportable_or_not() {

        class NonDeportable {}
        @ApplicationSingleton
        class Deportable {}


        var config = new ContextConfiguration();
        config.addClasses(List.of(NonDeportable.class, Deportable.class));
        config.setParentContext(mockParentContext);

        var triage = triageExecutor.execute(config);

        // NonDeportable should be in contextClasses, Deportable in deportedClasses
        assertTrue(triage.getContextClasses().contains(NonDeportable.class));
        assertTrue(!triage.getContextClasses().contains(Deportable.class));
        assertTrue(triage.getDeportedClasses().contains(Deportable.class));
        assertTrue(!triage.getDeportedClasses().contains(NonDeportable.class));
    }

    @Test
    void sealed_extension_context_must_load_deported_classes() {
        @ApplicationSingleton
        class ParentDeportable {}
        @ApplicationSingleton
        class LocalDeportable {}
        class NonDeportable {}

        // Use mock for parent context
        when(mockParentContext.getDeportedClasses()).thenReturn(Set.of(ParentDeportable.class));
        when(mockParentContext.getUuid()).thenReturn(UUID.randomUUID());

        var config = new ContextConfiguration();
        config.addClasses(List.of(NonDeportable.class, LocalDeportable.class));
        config.setSealed(true);
        config.setParentContext(mockParentContext);

        var triage = triageExecutor.execute(config);

        // Sealed extension should load deported classes locally
        assertTrue(triage.getContextClasses().contains(ParentDeportable.class));
        assertTrue(triage.getContextClasses().contains(LocalDeportable.class));
    }

    @Test
    void root_context_must_deport_local_deportable_classes() {
        @ApplicationSingleton
        class Deportable {}
        class NonDeportable {}


        // Simulate root context by setting parentContext and using
        // Extension.BOOT_ID as parent context Id
        Mockito.when(mockParentContext.getUuid()).thenReturn(Extension.BOOT_ID);

        var config = new ContextConfiguration();
        config.setParentContext(mockParentContext);
        config.addClasses(List.of(NonDeportable.class, Deportable.class));

        // If needed, set the parent context ID via reflection or by extending ContextConfiguration for testing
        // (Assuming the logic in ClassTriageExecutor uses parentContext == null to detect root context)
        var triage = triageExecutor.execute(config);

        // Root extension should deport local deportable classes
        assertTrue(triage.getDeportedClasses().contains(Deportable.class));
        assertTrue(!triage.getDeportedClasses().contains(NonDeportable.class));
    }

    @Test
    void deportable_extension_classes_must_be_deported_to_the_next_sealed_extension() {
        class ChildNonDeportable {}
        @ApplicationSingleton
        class ChildDeportable {}

        var config = new ContextConfiguration();
        config.addChildrenClasses(List.of(ChildNonDeportable.class, ChildDeportable.class));
        //config.setParentContext(mockParentContext);

        var triage = triageExecutor.execute(config);

        // ChildNonDeportable should be in contextClasses, ChildDeportable in deportedClasses
        assertTrue(triage.getContextClasses().contains(ChildNonDeportable.class));
        assertTrue(triage.getDeportedClasses().contains(ChildDeportable.class));
    }
}
