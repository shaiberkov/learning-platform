package org.example.learningprojectserver;

import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cache manager must be Ehcache through JCache, configured by ehcache.xml (TTL and heap limits).
 * Without the JSR-107 API on the classpath Spring Boot silently falls back to an unbounded
 * ConcurrentMapCacheManager that creates any cache on demand.
 */
@SpringBootTest(classes = CacheConfigurationTest.Config.class)
class CacheConfigurationTest {

    @Configuration
    @EnableCaching
    @ImportAutoConfiguration(CacheAutoConfiguration.class)
    static class Config {}

    /** Not Serializable, like most DTOs in this code base. */
    static final class PlainDto {
        final String name;
        PlainDto(String name) { this.name = name; }
    }

    @Autowired CacheManager cacheManager;

    @Test
    void usesEhcacheWithEveryAliasAndNoImplicitCaches() {
        assertInstanceOf(JCacheCacheManager.class, cacheManager);
        assertEquals(Set.of("studentSchedule", "aiPromptCache", "upcomingEvents", "teacherSchedule", "schoolDTO",
                        "receivedMessages", "allSchools", "classRoomSchedule", "studentTestsStatus", "schoolFeatureFlags"),
                Set.copyOf(cacheManager.getCacheNames()));
        assertNull(cacheManager.getCache("doesNotExist"));
    }

    @Test
    void storesNonSerializableValuesByReference() {
        Cache cache = cacheManager.getCache("upcomingEvents");
        assertNotNull(cache);
        BasicResponse response = new BasicResponse(true, null);
        response.setData(List.of(new PlainDto("a")));
        cache.put("STUDENT_1", response);
        assertSame(response, cache.get("STUDENT_1", BasicResponse.class));
        cache.evict("STUDENT_1");
        assertNull(cache.get("STUDENT_1"));
    }
}
