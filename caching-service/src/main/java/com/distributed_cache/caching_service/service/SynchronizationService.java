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
    private final ModelMapper modelMapper;

    public SynchronizationService(CacheStore cacheStore, CacheRepository cacheRepository, DataObjectFactory dataObjectFactory, ModelMapper modelMapper) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.dataObjectFactory = dataObjectFactory;
        this.modelMapper = modelMapper;
    }


    public void snapshotSync(){

    }

    public void outboxSync() throws Exception {

        List<CacheEventSource> cacheEventSourceList = cacheRepository.getCacheEvent(0L);


        for (CacheEventSource cacheEvent : cacheEventSourceList) {
            String key = cacheEvent.getKey();
//            String value = cacheEvent.getValue();
//            OperationType operationType = cacheEvent.getOperationType();
            DataObjectType dataObjectType = cacheEvent.getDataObjectType();
            Integer version = cacheEvent.getVersionNumber();

            if (cacheStore.inMemoryCache.get(key).getVersionNumber() < version) {

                DataObject dataObject = dataObjectFactory.create(dataObjectType);
                RequestDTO requestDTO = modelMapper.map(cacheEvent, RequestDTO.class);
                dataObject.execute(requestDTO);

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
