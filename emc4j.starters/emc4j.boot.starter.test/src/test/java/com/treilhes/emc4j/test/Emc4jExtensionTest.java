package com.treilhes.emc4j.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@Emc4jTest
@ContextConfiguration(classes = { Emc4jExtensionTest.Config.class })
@Disabled
class Emc4jExtensionTest {

    @Configuration
    static class Config {

    }
    @Test
    void test() {
        fail("Not yet implemented");
    }

}
