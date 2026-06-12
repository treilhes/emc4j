package com.treilhes.emc4j.boot.api.utils;

import org.springframework.beans.CachedIntrospectionResults;

import com.fasterxml.jackson.databind.type.TypeFactory;

public class CacheCleaner {

    public static void clearCaches(ClassLoader classLoader) {
        // AopProxy cache is managed by -Dcglib.useCache=false

        com.treilhes.emc4j.spring.core.patch.PatchLink.clearCaches(classLoader);
        com.treilhes.emc4j.spring.data.commons.patch.PatchLink.clearCaches(classLoader);
        CachedIntrospectionResults.clearClassLoader(classLoader);
        TypeFactory.defaultInstance().clearCache();
    }
}
