
package com.distributed_cache.caching_service.utils;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Data
public class CacheStore {

    public ConcurrentHashMap<String, CacheValue> inMemoryCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ReentrantReadWriteLock> inMemoryLock = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, PriorityQueue<CacheEventSource>> inMemoryMinHeap = new ConcurrentHashMap<>();

    private final GlobalVariableClass globalVariableClass;

    public ReentrantReadWriteLock getLock(String key) {
        return inMemoryLock.computeIfAbsent(
                key,
                k -> new ReentrantReadWriteLock()
        );
    }


    public void bufferKey(String key, CacheEventSource cacheEventSource){
        PriorityQueue<CacheEventSource> minHeap =
                inMemoryMinHeap.computeIfAbsent(
                        key,
                        k -> new PriorityQueue<>(
                                Comparator.comparingLong(CacheEventSource::getVersionNumber)
                        )
                );

        synchronized (minHeap) {
            minHeap.add(cacheEventSource);
        }
    }


    public void bufferClear(String key, Integer version) {
        PriorityQueue<CacheEventSource> minHeap = inMemoryMinHeap.get(key);

        if (minHeap == null) {
            return;
        }

        synchronized (minHeap) {
            while (!minHeap.isEmpty() && minHeap.peek().getVersionNumber() < version) {
                minHeap.poll();
            }
        }
    }


    public CacheEventSource bufferGetSequence(String key, Integer currentVersion){

        PriorityQueue<CacheEventSource> minHeap = inMemoryMinHeap.get(key);
        CacheEventSource cacheEventSource = null;

        if (minHeap == null || minHeap.isEmpty()) {
            return null;
        }

        synchronized (minHeap) {

            if(minHeap.peek().getVersionNumber().equals(currentVersion + 1)){
                cacheEventSource = minHeap.peek();
                minHeap.poll();
                return cacheEventSource;
            }
        }

        return cacheEventSource;
    }
}
