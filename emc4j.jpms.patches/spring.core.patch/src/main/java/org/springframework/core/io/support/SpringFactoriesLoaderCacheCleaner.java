package org.springframework.core.io.support;

public class SpringFactoriesLoaderCacheCleaner {

        public static void clearCache(ClassLoader classLoader) {
            // Clearing SpringFactoriesLoader cache
            SpringFactoriesLoader.cache.clear();
            //SpringFactoriesLoader.cache.remove(classLoader);
        }
}
