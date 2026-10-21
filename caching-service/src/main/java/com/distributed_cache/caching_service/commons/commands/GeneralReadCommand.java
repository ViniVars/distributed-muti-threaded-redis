package com.distributed_cache.caching_service.commons.commands;

import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public abstract class GeneralReadCommand {

    protected final CacheStore cacheStore;

    public GeneralReadCommand(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    public Object read(String key, String path){

        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.readLock().lock();

        try{

            CacheValue current = cacheStore.inMemoryCache.get(key);

            return (current == null)
                    ? null
                    : current.getValue();

        }
        finally {
            lock.readLock().unlock();
        }
    }
}
