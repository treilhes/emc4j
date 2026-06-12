package com.treilhes.emc4j.spring.data.commons.patch;

import org.springframework.data.core.ClassTypeInformationCacheCleaner;
import org.springframework.data.core.TypeDiscovererCacheCleaner;

public class PatchLink {
    public static void clearCaches(ClassLoader classLoader) {
        ClassTypeInformationCacheCleaner.clean(classLoader);
        TypeDiscovererCacheCleaner.clean(classLoader);
    }
}
