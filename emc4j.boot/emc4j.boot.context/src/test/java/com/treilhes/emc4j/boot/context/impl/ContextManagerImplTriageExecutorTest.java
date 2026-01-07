package com.treilhes.emc4j.boot.context.impl;

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
        assert triage.getContextClasses().contains(NonDeportable.class);
        assert !triage.getContextClasses().contains(Deportable.class);
        assert triage.getDeportedClasses().contains(Deportable.class);
        assert !triage.getDeportedClasses().contains(NonDeportable.class);
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
        assert triage.getContextClasses().contains(ParentDeportable.class);
        assert triage.getContextClasses().contains(LocalDeportable.class);
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
        assert triage.getDeportedClasses().contains(Deportable.class);
        assert !triage.getDeportedClasses().contains(NonDeportable.class);
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
        assert triage.getContextClasses().contains(ChildNonDeportable.class);
        assert triage.getDeportedClasses().contains(ChildDeportable.class);
    }
}
