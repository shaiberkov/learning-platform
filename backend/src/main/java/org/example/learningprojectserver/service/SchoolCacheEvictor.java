package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Evicts the per-user and per-class caches that the school-administration writes
 * (timetable builder, bulk import, year close) change. Keys match the @Cacheable keys
 * listed in docs/CACHING.md. A cache missing from ehcache.xml is skipped instead of throwing.
 */
@Component
@RequiredArgsConstructor
public class SchoolCacheEvictor {

    private final CacheManager cacheManager;

    public void teacherSchedule(String teacherId) {
        evict("teacherSchedule", teacherId);
    }

    public void classRoomSchedule(String schoolCode, String classRoomName) {
        evict("classRoomSchedule", schoolCode + "_" + classRoomName);
    }

    public void studentSchedule(String studentId) {
        evict("studentSchedule", studentId);
    }

    /** A student's dashboard feed and tests status, both of which follow the student's class. */
    public void studentDashboard(String studentId) {
        evict("upcomingEvents", "STUDENT_" + studentId);
        evict("studentTestsStatus", studentId);
    }

    public void schoolDTO(String schoolManagerId) {
        if (schoolManagerId != null) {
            evict("schoolDTO", schoolManagerId);
        }
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
