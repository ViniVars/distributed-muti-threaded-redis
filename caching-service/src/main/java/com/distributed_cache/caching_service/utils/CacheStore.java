
package com.distributed_cache.caching_service.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Data
public class CacheStore {

    public ConcurrentHashMap<String, CacheValue> inMemoryCache =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ReentrantReadWriteLock> inMemoryLock =
            new ConcurrentHashMap<>();

    public ReentrantReadWriteLock getLock(String key) {
        return inMemoryLock.computeIfAbsent(
                key,
                k -> new ReentrantReadWriteLock()
        );
    }
}
