package org.springframework.data.core;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentLruCache;

public class TypeDiscovererCacheCleaner {

    private static Field cacheField;
    private static Field innerMapField;
    private static Field variableResolverField;
    private static ConcurrentLruCache<ResolvableType, TypeInformation<?>> cache;
    private static ConcurrentMap<ResolvableType, ?> innerMap;

    static {
        try {
            cacheField = TypeDiscoverer.class.getDeclaredField("CACHE");
            innerMapField = ConcurrentLruCache.class.getDeclaredField("cache");

            cacheField.setAccessible(true);
            innerMapField.setAccessible(true);

            cache = (ConcurrentLruCache<ResolvableType, TypeInformation<?>>) cacheField.get(null);
            innerMap = (ConcurrentMap<ResolvableType, ?>) innerMapField.get(cache);

        } catch (Exception e) {
            throw new RuntimeException("Failed to access cache fields", e);
        }
    }


    /**
     * Access fields:
     * ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>> cache
     *
     * This method is used to clean the cache of TypeDiscoverer for a specific classloader.
     * @param loader the classloader for which to clean the cache
     */
    public static void clean(ClassLoader loader) {
        try {


            var b1 = innerMap.size();
            var b3 = cache.size();

            cache.clear();
            innerMap.clear();

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to access cache fields", e);
        }
    }

}
