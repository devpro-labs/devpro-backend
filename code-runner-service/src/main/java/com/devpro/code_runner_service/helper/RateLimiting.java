package com.devpro.code_runner_service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RateLimiting {

    @Value("${limits.user}")
    private String max_user_req; //per minute

    @Value("${limits.global.req}")
    private String max_global_req;

    @Value("${limits.global.time}")
    private String global_req_time;

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiting(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean isQuotaExceed(String userId) {
        try {


            int maxGlobal = Integer.parseInt(max_global_req);
            int maxUser = Integer.parseInt(max_user_req);
            long globalWindow = Long.parseLong(global_req_time);

            // global
            String globalKey = "rate-limiting:global";
            Long globalCount = redisTemplate.opsForValue().increment(globalKey);

            if (globalCount != null && globalCount == 1) {
                redisTemplate.expire(globalKey, Duration.ofSeconds(globalWindow));
            }

            if (globalCount != null && globalCount > maxGlobal) {
                return true;
            }

            // user
            String userKey = "rate-limiting:user:" + userId;
            Long userCount = redisTemplate.opsForValue().increment(userKey);

            if (userCount != null && userCount == 1) {
                redisTemplate.expire(userKey, Duration.ofMinutes(1));
            }

            return userCount != null && userCount > maxUser;

        } catch (Exception e) {
            log.error("Error in isQuotaExceed: {}", e.getMessage());
            return true;
        }
    }

}
