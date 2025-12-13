package achlaq.co.transactionreconengine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final StringRedisTemplate redisTemplate;

    @PostMapping("/{userId}")
    public String blockUser(@PathVariable Long userId) {
        redisTemplate.opsForSet().add("BLACKLIST_USERS", String.valueOf(userId));
        return "User " + userId + " has been blacklisted!";
    }

    @DeleteMapping("/{userId}")
    public String unblockUser(@PathVariable Long userId) {
        redisTemplate.opsForSet().remove("BLACKLIST_USERS", String.valueOf(userId));
        return "User " + userId + " is now clean.";
    }

    @GetMapping("/{userId}")
    public boolean checkUser(@PathVariable Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember("BLACKLIST_USERS", String.valueOf(userId))
        );
    }
}
