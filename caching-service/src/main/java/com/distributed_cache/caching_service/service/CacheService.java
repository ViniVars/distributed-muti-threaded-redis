package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.commons.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.utils.DataObjectType;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final DataObjectFactory dataObjectFactory;

    public CacheService(DataObjectFactory dataObjectFactory) {
        this.dataObjectFactory = dataObjectFactory;
    }


    public void cacheWriteService(RequestDTO requestDTO) throws Exception {
        DataObjectType dataObjectType = requestDTO.getDataObjectType();
        DataObject dataObject = dataObjectFactory.create(dataObjectType);
        dataObject.execute(requestDTO, false);
    }

    public Object cacheReadService(RequestDTO requestDTO) throws Exception {
        DataObjectType dataObjectType = requestDTO.getDataObjectType();
        DataObject dataObject = dataObjectFactory.create(dataObjectType);
        return dataObject.read(requestDTO.getKey(), requestDTO.getPath());
    }

}
