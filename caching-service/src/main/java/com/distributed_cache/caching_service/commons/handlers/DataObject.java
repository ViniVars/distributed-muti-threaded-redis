package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.dto.RequestDTO;

public interface DataObject {
    public Boolean execute(RequestDTO requestDTO, Boolean isSync) throws Exception;
    public Object read(String key, String path) throws Exception;
}
