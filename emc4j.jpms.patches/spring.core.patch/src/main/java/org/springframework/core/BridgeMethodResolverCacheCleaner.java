package org.springframework.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class BridgeMethodResolverCacheCleaner {

    private static Field cacheField;
    private static Map<Object, Method> cache;

    static {
        try {
            cacheField = BridgeMethodResolver.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            cache = (Map<Object, Method>) cacheField.get(null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to access cache fields", e);
        }
    }

    /**
     * This method is used to clean the cache of BridgeMethodResolver for a specific
     * classloader.
     *
     * @param loader the classloader for which to clean the cache
     */
    public static void clean(ClassLoader loader) {
//        try {
//            cache.entrySet().removeIf(entry -> {
//                return entry.getValue().getDeclaringClass().getClassLoader() == loader;
//            });
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to clear BridgeMethodResolver cache", e);
//        }
        cache.clear();
    }
}
