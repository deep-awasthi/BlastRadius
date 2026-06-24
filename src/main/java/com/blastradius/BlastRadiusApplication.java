package com.blastradius;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * BlastRadius - Engineering Dependency Intelligence Platform
 * "Ask your codebase anything."
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class BlastRadiusApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlastRadiusApplication.class, args);
    }
}
