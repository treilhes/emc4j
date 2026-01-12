package com.treilhes.emc4j.test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.annotation.LocalContextOnly;

@Emc4jTest(
        classes = {
                EmContextTest.TestComponent.class
        },
        context = @Emc4jCoreContext(
                classes = {
                        EmContextTest.TestComponent.class
                },
                extensions = {
                        @Emc4jExtensionContext(
                                uuid = EmContextTest.CORE_EXT_ID,
                                classes = {
                                        EmContextTest.TestComponent.class
                                }
                        )
                },
                applications = {
                        @Emc4jApplicationContext(
                                uuid = EmContextTest.APP_ID,
                                classes = {
                                        EmContextTest.TestComponent.class
                                }
                        )
                }
        )
)
class EmContextTest {
    static final String BOOT_ID = "00000000-0000-0000-0000-000000000000";
    static final String CORE_ID = "00000000-0000-0000-0000-000000000001";
    static final String CORE_EXT_ID = "20638225-7e56-4272-a513-000000000001";
    static final String APP_ID = "20638225-7e56-4272-a513-000000000003";

    static class TestComponent {

        private EmContext emContext;

        protected TestComponent(@LocalContextOnly EmContext emContext) {
            this.emContext = emContext;
        }

        public String hello() {
            return emContext.getUuid().toString();
        }
    }

    @Test
    void ensure_emcontext_annotation_return_null_for_unknown_id(@EmInject("20638225-7e56-4272-0000-000000000000") EmContext notFoundContext) {
        assertTrue(notFoundContext == null);
    }

    @Test
    void ensure_emcontext_annotation_return_the_right_context(
            EmContext bootContext,
            @EmInject EmContext bootContext2,
            @EmInject(BOOT_ID) EmContext bootContext3,
            @EmInject(CORE_ID) EmContext coreContext,
            @EmInject(CORE_EXT_ID) EmContext extContext,
            @EmInject(APP_ID) EmContext appContext
            ) {

        assertEquals(BOOT_ID, bootContext.getId());
        assertEquals(BOOT_ID, bootContext2.getId());
        assertEquals(BOOT_ID, bootContext3.getId());
        assertTrue(bootContext == bootContext2);
        assertTrue(bootContext == bootContext3);

        assertEquals(CORE_ID,  coreContext.getId());
        assertEquals(CORE_EXT_ID, extContext.getId());
        assertEquals(APP_ID, appContext.getId());

    }

    @Test
    void ensure_default_emcontext_annotation_return_the_boot_component(
            @EmInject TestComponent bootComponent,
            @EmInject(BOOT_ID) TestComponent bootComponent2
            ) {
        assertEquals(BOOT_ID, bootComponent.hello());
        assertTrue(bootComponent == bootComponent2);
    }

    @Test
    void ensure_emcontext_annotation_return_the_right_context_and_component(
            @EmInject(BOOT_ID) TestComponent bootComponent,
            @EmInject(CORE_ID) TestComponent coreComponent,
            @EmInject(CORE_EXT_ID) TestComponent extComponent,
            @EmInject(APP_ID) TestComponent appComponent
            ) {
        assertEquals(BOOT_ID, bootComponent.hello());
        assertEquals(CORE_ID, coreComponent.hello());
        assertEquals(CORE_EXT_ID, extComponent.hello());
        assertEquals(APP_ID, appComponent.hello());
    }


}
