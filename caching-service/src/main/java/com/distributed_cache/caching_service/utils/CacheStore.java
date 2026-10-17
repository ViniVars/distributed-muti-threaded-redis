package com.distributed_cache.caching_service.utils;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Data
public class CacheStore {
    public ConcurrentHashMap<String, CacheValue> inMemoryCache = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Object> inMemoryLock = new ConcurrentHashMap<>();


    public Object getLock(String key){
        return inMemoryLock.computeIfAbsent(key, k -> new Object());
    }
}
