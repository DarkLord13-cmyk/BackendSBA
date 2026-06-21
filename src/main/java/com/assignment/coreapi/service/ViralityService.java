package com.assignment.coreapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ViralityService {

    private final StringRedisTemplate redisTemplate;

    public void updateViralityScore(Long postId, int points) {
        String key = "post:" + postId + ":virality_score";
        redisTemplate.opsForValue().increment(key, points);
    }

    public void checkAndIncrementBotHorizontalCap(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count > 100) {
            // Revert increment if limit exceeded
            redisTemplate.opsForValue().decrement(key);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Post has reached the maximum of 100 bot replies.");
        }
    }

    public void checkVerticalCap(int depthLevel) {
        if (depthLevel > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment thread cannot go deeper than 20 levels.");
        }
    }

    public void checkAndSetBotCooldownCap(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        Boolean isAllowed = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        
        if (Boolean.FALSE.equals(isAllowed)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot is on cooldown for interacting with this human.");
        }
    }
}
