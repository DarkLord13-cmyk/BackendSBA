package com.assignment.coreapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final StringRedisTemplate redisTemplate;

    public void handleBotInteractionNotification(Long userId, String botName) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";
        String listKey = "user:" + userId + ":pending_notifs";

        Boolean isCooldownActive = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(isCooldownActive)) {
            // Push to pending list
            redisTemplate.opsForList().rightPush(listKey, "Bot " + botName + " interacted with your post");
        } else {
            // Log immediate notification and set cooldown
            log.info("Push Notification Sent to User {}: Bot {} interacted with your post", userId, botName);
            redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
        }
    }

    // Cron expression: runs every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void sweepPendingNotifications() {
        // In a real production scenario with many keys, SCAN should be used instead of KEYS
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String userIdStr = key.split(":")[1];
            List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
            if (messages != null && !messages.isEmpty()) {
                int count = messages.size();
                log.info("Summarized Push Notification for User {}: Bot {} and {} others interacted with your posts.", 
                        userIdStr, extractBotName(messages.get(0)), count - 1);
                
                // Clear the list
                redisTemplate.delete(key);
            }
        }
    }

    private String extractBotName(String message) {
        // Basic extraction for logging purposes: "Bot X interacted with your post" -> "X"
        try {
            return message.split(" ")[1];
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
