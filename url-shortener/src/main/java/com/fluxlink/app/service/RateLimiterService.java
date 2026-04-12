package com.fluxlink.app.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // 5 requests per minute
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60000;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(Long userId) {
        String key = "ratelimit:" + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_MS;

        // Remove older timestamps
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count requests in current window
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= MAX_REQUESTS) {
            return false;
        }

        // Add current request timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, java.time.Duration.ofMillis(WINDOW_MS));
        
        return true;
    }
}
