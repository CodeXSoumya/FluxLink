package com.fluxlink.app.service;

import com.fluxlink.app.model.Url;
import com.fluxlink.app.model.User;
import com.fluxlink.app.repository.UrlRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;

    public UrlService(UrlRepository urlRepository, Base62Encoder base62Encoder, RedisTemplate<String, String> redisTemplate) {
        this.urlRepository = urlRepository;
        this.base62Encoder = base62Encoder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public String shortenUrl(String longUrl, User user) {
        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setUser(user);
        
        url = urlRepository.saveAndFlush(url);
        
        String shortCode = base62Encoder.encode(url.getId());
        url.setShortCode(shortCode);
        
        urlRepository.save(url);
        
        // Cache the mapping
        redisTemplate.opsForValue().set("url:" + shortCode, longUrl, 1, TimeUnit.DAYS);
        
        return shortCode;
    }

    public String getLongUrl(String shortCode) {
        String cacheKey = "url:" + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedUrl != null) {
            return cachedUrl;
        }

        Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
        if (urlOpt.isPresent()) {
            String longUrl = urlOpt.get().getLongUrl();
            redisTemplate.opsForValue().set(cacheKey, longUrl, 1, TimeUnit.DAYS);
            return longUrl;
        }

        return null;
    }
}
