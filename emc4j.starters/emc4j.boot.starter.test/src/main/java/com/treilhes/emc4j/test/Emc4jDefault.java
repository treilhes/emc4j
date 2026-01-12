package com.treilhes.emc4j.test;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

public @interface Emc4jDefault {
    String[] properties() default {};

    WebEnvironment webEnvironment() default WebEnvironment.NONE;
    boolean enableJpa() default false;
    boolean enableAop() default false;

    boolean loadDefaultScopes() default true;
    Class<?>[] classes() default {};
    Emc4jCoreContext context() default @Emc4jCoreContext;
}
