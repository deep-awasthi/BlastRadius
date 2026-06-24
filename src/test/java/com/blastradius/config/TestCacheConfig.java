package com.blastradius.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test-specific cache configuration that replaces Redis/Caffeine with
 * a simple in-memory ConcurrentMapCacheManager (no external dependencies).
 */
@Configuration
@Profile("test")
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                "dashboard", "search", "components", "scan-summary",
                "impact-analysis", "dependency-graph");
    }
}
