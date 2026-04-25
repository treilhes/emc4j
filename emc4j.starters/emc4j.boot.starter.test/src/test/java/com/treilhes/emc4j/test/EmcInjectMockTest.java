package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.treilhes.emc4j.boot.api.context.EmContext;

@Emc4jTest
class EmcInjectMockTest {

    interface ITest {
        String get();
    }

    @EmcInjectMock
    ITest boot;


    @Test
    void must_mock_bean_into_boot_context_without_annotation_parameter(@EmcInject EmContext emContext) {
        assertNotNull(boot);
        assertTrue(Mockito.mockingDetails(boot).isMock());

        var names = emContext.getBeanFactory().getBeanNamesForType(ITest.class);
        assertEquals(1, names.length);

        var bean = emContext.getBean(ITest.class);
        assertTrue(Mockito.mockingDetails(bean).isMock());
        assertEquals(boot, bean);
    }

}
