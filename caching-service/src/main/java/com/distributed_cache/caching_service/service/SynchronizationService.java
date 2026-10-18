package com.distributed_cache.caching_service.service;


import com.distributed_cache.caching_service.commons.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.MapEntry;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SynchronizationService {


    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final DataObjectFactory dataObjectFactory;
    private final ObjectMapper objectMapper;

    public SynchronizationService(CacheStore cacheStore, CacheRepository cacheRepository, DataObjectFactory dataObjectFactory, ObjectMapper objectMapper) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.dataObjectFactory = dataObjectFactory;
        this.objectMapper = objectMapper;
    }





    public void snapshotSync(){



    }

    public void outboxSync() throws Exception {

        List<CacheEventSource> cacheEventSourceList = cacheRepository.getCacheEvent(0L);


        for (CacheEventSource cacheEvent : cacheEventSourceList) {
            String key = cacheEvent.getKey();
            String value = cacheEvent.getValue();
            OperationType operationType = cacheEvent.getOperationType();
            DataObjectType dataObjectType = cacheEvent.getDataObjectType();
            Integer version = cacheEvent.getVersionNumber();

            Integer inMemoryVersionNumber = cacheStore.inMemoryCache.get(key) == null ? 0 : cacheStore.inMemoryCache.get(key).getVersionNumber();

            if (inMemoryVersionNumber + 1 == version) {

                DataObject dataObject = dataObjectFactory.create(dataObjectType);
                RequestDTO requestDTO = new RequestDTO();
                requestDTO.setKey(key);
                requestDTO.setOperationType(operationType);
                requestDTO.setPath(cacheEvent.getPath());
                requestDTO.setValue(objectMapper.readValue(value, Object.class));
                dataObject.execute(requestDTO, true);
                cacheStore.inMemoryCache.get(key).setVersionNumber(version);
            }
        }

        for (Map.Entry<String, CacheValue> entry :
                cacheStore.inMemoryCache.entrySet()) {

            System.out.println("Key: " + entry.getKey());
            System.out.println("Value: " + entry.getValue().getValue());
            System.out.println("Version: " + entry.getValue().getVersionNumber());
        }

    }


}
