package com.sap.pfs.oauth.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPT = 5;

    private LoadingCache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        super();
        System.out.println("Attempting to login");

        attemptsCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build(new CacheLoader<String, Integer>() {
            public Integer load(String key) {
                return 0;
            }
        });
    }

    public void loginSucceeded(String key) {
        System.out.println("Login succeeded for " + key);
        System.out.println("Invalidating attemptsCache for " + key);
        attemptsCache.invalidate(key);
    }

    public void loginFailed(String key) {
        System.out.println("Login failed for " + key);
        int attempts = 0;
        try {
            attempts = attemptsCache.get(key);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(key, attempts);
    }

    public boolean isBlocked(String key) {
        try {
            return attemptsCache.get(key) >= MAX_ATTEMPT;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public void invalidateAttemptsCache(String key) {
        System.out.println("Invalidating attemptsCache for " + key);
        attemptsCache.invalidate(key);
    }
}