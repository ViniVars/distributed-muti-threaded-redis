package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;

public interface DataObject {
    public Boolean execute(RequestDTO requestDTO) throws Exception;
    public Boolean executeSync(CacheEventSource cacheEventSource) throws Exception;
    public Object read(String key, String path) throws Exception;
}
