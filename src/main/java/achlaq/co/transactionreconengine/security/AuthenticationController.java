package achlaq.co.transactionreconengine.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final JwtService jwtService;

    public AuthenticationController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Generates a JWT for testing purposes.
     * In a real application, this would be a login endpoint with username/password.
     * @param username The username to encode in the token.
     * @return A JWT.
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> getTestToken(@RequestParam String username) {
        String token = jwtService.generateToken(username);
        return ResponseEntity.ok(Map.of("token", token));
    }
}