package com.distributed_cache.caching_service.commons.commands;

import com.distributed_cache.caching_service.utils.CacheValue;
import org.springframework.stereotype.Component;

@Component
public class GeneralReadCommand {

    public Object execute(String key, String path){
        CacheValue current = cacheStore.inMemoryCache.get(key);



        return (current == null)
                ? null
                : current.getValue();
    }
}
