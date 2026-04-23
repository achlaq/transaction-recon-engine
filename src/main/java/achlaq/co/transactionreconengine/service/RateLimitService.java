package achlaq.co.transactionreconengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.transaction.rate-limit.max-per-minute:5}")
    private int maxTxPerMinute;

    public boolean isRateLimited(Long userId) {
        String key = "VELOCITY::" + userId;

        String luaScript = 
            "local current = redis.call('incr', KEYS[1]) " +
            "if current == 1 then " +
            "    redis.call('expire', KEYS[1], 60) " +
            "end " +
            "return current";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long count = redisTemplate.execute(script, Collections.singletonList(key));

        return count != null && count > maxTxPerMinute;
    }

    public boolean isUserBlacklisted(Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember("BLACKLIST_USERS", String.valueOf(userId))
        );
    }
}