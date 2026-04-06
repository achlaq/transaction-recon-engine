package achlaq.co.transactionreconengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.transaction.rate-limit.max-per-minute:5}")
    private int maxTxPerMinute;

    public boolean isRateLimited(Long userId) {
        String key = "VELOCITY::" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        return count != null && count > maxTxPerMinute;
    }

    public boolean isUserBlacklisted(Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember("BLACKLIST_USERS", String.valueOf(userId))
        );
    }
}