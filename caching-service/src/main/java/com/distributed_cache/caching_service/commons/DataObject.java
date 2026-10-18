package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.utils.OperationType;

public interface DataObject {
    public Boolean execute(RequestDTO requestDTO, Boolean isSync) throws Exception;
    public Object read(String key, String path) throws Exception;
}
