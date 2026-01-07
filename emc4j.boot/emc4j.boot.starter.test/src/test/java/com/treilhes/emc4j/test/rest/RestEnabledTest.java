package com.treilhes.emc4j.test.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.annotation.LocalContextOnly;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;
import com.treilhes.emc4j.boot.api.web.client.InternalRestClient;
import com.treilhes.emc4j.test.Emc4jApplicationContext;
import com.treilhes.emc4j.test.Emc4jCoreContext;
import com.treilhes.emc4j.test.Emc4jExtensionContext;
import com.treilhes.emc4j.test.Emc4jNestedExtensionContext;
import com.treilhes.emc4j.test.Emc4jTest;

import jakarta.inject.Inject;

@Emc4jTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                //"server.port=9999"
        },
        classes = {
                RestEnabledTest.TestController.class
        },
        context = @Emc4jCoreContext(
                uuid = RestEnabledTest.CORE_ID,
                classes = {
                        RestEnabledTest.TestController.class
                },
                extensions = {
                        @Emc4jExtensionContext(
                                uuid = RestEnabledTest.CORE_EXT_ID,
                                classes = {
                                        RestEnabledTest.TestController.class
                                },
                                extensions = {
                                        @Emc4jNestedExtensionContext(
                                                uuid = RestEnabledTest.CORE_NESTED_EXT_ID,
                                                classes = {
                                                        RestEnabledTest.TestController.class
                                                }
                                        )

                                }
                        )
                },
                applications = {
                        @Emc4jApplicationContext(
                                uuid = RestEnabledTest.APP_ID,
                                classes = {
                                        RestEnabledTest.TestController.class
                                },
                                extensions = {
                                        @Emc4jExtensionContext(
                                                uuid = RestEnabledTest.APP_EXT_ID,
                                                classes = {
                                                        RestEnabledTest.TestController.class
                                                },
                                                extensions = {
                                                        @Emc4jNestedExtensionContext(
                                                                uuid = RestEnabledTest.APP_NESTED_EXT_ID,
                                                                classes = {
                                                                        RestEnabledTest.TestController.class
                                                                }
                                                        )

                                                }
                                        )
                                }
                        )
                }
        )
)
class RestEnabledTest {
    static final String CORE_ID = "20638225-7e56-4272-a513-000000000000";
    static final String CORE_EXT_ID = "20638225-7e56-4272-a513-000000000001";
    static final String CORE_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000002";

    static final String APP_ID = "20638225-7e56-4272-a513-000000000003";
    static final String APP_EXT_ID = "20638225-7e56-4272-a513-000000000004";
    static final String APP_NESTED_EXT_ID = "20638225-7e56-4272-a513-000000000005";

    @RestController
    @RequestMapping("/test")
    static class TestController {

        private EmContext emContext;

        protected TestController(@LocalContextOnly EmContext emContext) {
            this.emContext = emContext;
        }

        @GetMapping("/uuid")
        public String hello() {
            return emContext.getUuid().toString();
        }
    }

    @Inject
    InternalRestClient restClient;

    @Test
    void test() throws URISyntaxException, IOException {
        String bootUuid = restClient.get(Extension.BOOT_ID, "/test/uuid").execute();
        String coreUuid = restClient.get(UUID.fromString(CORE_ID), "/test/uuid").execute();
        String coreExtUuid = restClient.get(UUID.fromString(CORE_EXT_ID), "/test/uuid").execute();
        String coreNestedExtUuid = restClient.get(UUID.fromString(CORE_NESTED_EXT_ID), "/test/uuid").execute();
        String appUuid = restClient.get(UUID.fromString(APP_ID), "/test/uuid").execute();
        String appExtUuid = restClient.get(UUID.fromString(APP_EXT_ID), "/test/uuid").execute();
        String appNestedExtUuid = restClient.get(UUID.fromString(APP_NESTED_EXT_ID), "/test/uuid").execute();

        assertEquals(Extension.BOOT_ID.toString(), bootUuid);
        assertEquals(CORE_ID, coreUuid);
        assertEquals(CORE_EXT_ID, coreExtUuid);
        assertEquals(CORE_NESTED_EXT_ID, coreNestedExtUuid);
        assertEquals(APP_ID, appUuid);
        assertEquals(APP_EXT_ID, appExtUuid);
        assertEquals(APP_NESTED_EXT_ID, appNestedExtUuid);

    }

}
