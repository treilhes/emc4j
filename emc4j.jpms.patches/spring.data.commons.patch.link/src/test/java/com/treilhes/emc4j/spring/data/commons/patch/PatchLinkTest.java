package com.treilhes.emc4j.spring.data.commons.patch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.mapping.Alias;

class PatchLinkTest {

    /**
     * Simple test to ensure that the cache cleaning process of the SimpleTypeInformationMapper
     * doesn't throw an exception when it tries to clean the cache. This is important because if
     * it throws an exception, it could cause issues in the application when trying to clear the cache.
     *
     * As clearTypeInformationCache is a static method handling ugly internal static access, we can't easily mock or spy.
     * So this test is voluntary simple and incomplete.
     */
    @Test
    void type_information_cache_cleaning_musnt_throw_exception() {
        // indirect way to test that the cache cleaning process of the SimpleTypeInformationMapper doesn't throw an exception when it tries to clean the cache
        SimpleTypeInformationMapper typeInformationMapper = new SimpleTypeInformationMapper();
        typeInformationMapper.resolveTypeFrom(Alias.of("com.treilhes.emc4j.spring.data.commons.patch.PatchLinkTest"));
        typeInformationMapper.resolveTypeFrom(Alias.of("java.lang.String"));

        assertDoesNotThrow(() -> PatchLink.clearCaches(PatchLinkTest.class.getClassLoader()));
    }

}
