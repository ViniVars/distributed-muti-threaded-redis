package com.distributed_cache.caching_service.commons.commands;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.stereotype.Component;


@Component
public interface WriteCommand {



    Boolean execute(RequestDTO requestDTO, Integer updationValue) throws Exception;
    Boolean executeSync(CacheEventSource cacheEventSource) throws Exception;


}
