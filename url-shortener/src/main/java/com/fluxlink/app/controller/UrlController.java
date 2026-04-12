package com.fluxlink.app.controller;

import com.fluxlink.app.model.User;
import com.fluxlink.app.repository.UserRepository;
import com.fluxlink.app.service.RateLimiterService;
import com.fluxlink.app.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;
    private final UserRepository userRepository;

    public UrlController(UrlService urlService, RateLimiterService rateLimiterService, UserRepository userRepository) {
        this.urlService = urlService;
        this.rateLimiterService = rateLimiterService;
        this.userRepository = userRepository;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // Fallback user logic for testing if Auth is not strictly injected
        final Long finalUserId = (userId == null) ? 1L : userId;

        if (!rateLimiterService.isAllowed(finalUserId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many requests. Try again later.");
        }

        String longUrl = body.get("longUrl");
        if (longUrl == null || longUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("longUrl is required");
        }

        User user = userRepository.findById(finalUserId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(finalUserId);
            newUser.setEmail("user" + finalUserId + "@test.com");
            newUser.setPasswordHash("mock");
            return userRepository.save(newUser);
        });

        String shortCode = urlService.shortenUrl(longUrl, user);
        return ResponseEntity.ok(Map.of("shortId", shortCode, "shortUrl", "http://localhost:8080/" + shortCode));
    }

    @GetMapping("/{shortId}")
    public ResponseEntity<?> redirect(@PathVariable String shortId) {
        if (shortId.equals("favicon.ico") || shortId.equals("actuator")) {
            return ResponseEntity.notFound().build();
        }

        String longUrl = urlService.getLongUrl(shortId);
        
        if (longUrl == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
