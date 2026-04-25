package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Emc4jTest
class EmcInjectSpyTest {

    @EmcInjectSpy
    Emc4jTest.Application1Bean bean;


    @Test
    void must_spy_bean_from_boot_context_without_annotation_parameter() {
        assertNotNull(bean);
        assertTrue(Mockito.mockingDetails(bean).isSpy());
    }

}
