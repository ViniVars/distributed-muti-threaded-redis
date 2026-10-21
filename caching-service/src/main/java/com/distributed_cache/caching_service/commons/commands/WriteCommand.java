package com.distributed_cache.caching_service.commons.commands;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.stereotype.Component;


@Component
public interface WriteCommand  {
    CacheEventSource persistDB(RequestDTO requestDTO) throws Exception;
    Boolean syncInMemory(CacheEventSource cacheEventSource) throws Exception;
}
