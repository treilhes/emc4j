open module emc4j.boot.api {

    exports com.treilhes.emc4j.boot.api.aop;
    exports com.treilhes.emc4j.boot.api.context;
    exports com.treilhes.emc4j.boot.api.context.annotation;
    exports com.treilhes.emc4j.boot.api.jpa;
    exports com.treilhes.emc4j.boot.api.layer;
    exports com.treilhes.emc4j.boot.api.loader;
    exports com.treilhes.emc4j.boot.api.loader.extension;
    exports com.treilhes.emc4j.boot.api.maven;
    exports com.treilhes.emc4j.boot.api.platform;
    exports com.treilhes.emc4j.boot.api.registry;
    exports com.treilhes.emc4j.boot.api.registry.model;
    exports com.treilhes.emc4j.boot.api.splash;
    exports com.treilhes.emc4j.boot.api.utils;
    exports com.treilhes.emc4j.boot.api.web.client;

    requires emc4j.boot.starter;

    requires emc4j.spring.core.patch.link;
    requires emc4j.hibernate.core.patch.link;
}