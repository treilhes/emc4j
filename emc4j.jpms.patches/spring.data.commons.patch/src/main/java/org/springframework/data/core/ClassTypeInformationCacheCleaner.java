package org.springframework.data.core;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentLruCache;

public class ClassTypeInformationCacheCleaner {

    private static Field cacheField;
    private static Field resolvableTypeCacheField;
    private static Field innerMapField;
    private static ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>> cache;
    private static ConcurrentLruCache<Class<?>, ResolvableType> resolvableTypeCache;
    private static ConcurrentMap<Class<?>, ?> innerMap;

    static {
        try {
            cacheField = ClassTypeInformation.class.getDeclaredField("cache");
            resolvableTypeCacheField = ClassTypeInformation.class.getDeclaredField("resolvableTypeCache");
            innerMapField = ConcurrentLruCache.class.getDeclaredField("cache");

            cacheField.setAccessible(true);
            resolvableTypeCacheField.setAccessible(true);
            innerMapField.setAccessible(true);

            cache = (ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>>) cacheField.get(null);
            resolvableTypeCache = (ConcurrentLruCache<Class<?>, ResolvableType>) resolvableTypeCacheField.get(null);
            innerMap = (ConcurrentMap<Class<?>, ?>) innerMapField.get(resolvableTypeCache);

        } catch (Exception e) {
            throw new RuntimeException("Failed to access cache fields", e);
        }
    }


    /**
     * Access fields:
     * ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>> cache
     * ConcurrentLruCache<Class<?>, ResolvableType> resolvableTypeCache
     *
     * To iterate over values it is also necessary to access the ConcurrentLruCache field:
     * ConcurrentMap<K, Node<K, V>> cache
     *
     * This method is used to clean the cache of ClassTypeInformation for a specific classloader.
     * @param loader the classloader for which to clean the cache
     */
    public static void clean(ClassLoader loader) {
        try {

//            var b1 = innerMap.size();
//            var b2 = resolvableTypeCache.size();
//            var b3 = cache.size();
//
//            var toRemove = innerMap.entrySet().stream()
//                    .peek(e -> System.out.println("CHECKING:" + e.getKey()))
//                    .filter(entry -> entry.getKey().getClassLoader() == loader)
//                    .map(entry -> entry.getKey())
//                    .peek(e -> System.out.println("SELECTED:" + e))
//                    .toList();
//
//            toRemove.forEach(key -> {
//                var resolvableType = resolvableTypeCache.get(key);
//                resolvableTypeCache.remove(key);
//                cache.remove(resolvableType);
//            });
//
//            var a1 = innerMap.size();
//            var a2 = resolvableTypeCache.size();
//            var a3 = cache.size();
//
//            var d1 = b1 - a1;
//            var d2 = b2 - a2;
//            var d3 = b3 - a3;
//            if (d1 > 0 || d2 > 0 || d3 > 0) {
//                System.out.println("Removing " + toRemove.size());
//                System.out.println("Cleared ClassTypeInformation cache for classloader " + loader + ": " + d1 + " entries removed from innerMap, " + d2 + " entries removed from resolvableTypeCache, " + d3 + " entries removed from cache");
//            }
            cache.clear();
            resolvableTypeCache.clear();
            innerMap.clear();

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to access cache fields", e);
        }
    }
}
