
package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.commons.handlers.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.DataObjectType;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class CacheService {

    private final DataObjectFactory dataObjectFactory;
    private final CacheStore cacheStore;

    public CacheService(
            DataObjectFactory dataObjectFactory,
            CacheStore cacheStore) {

        this.dataObjectFactory = dataObjectFactory;
        this.cacheStore = cacheStore;
    }

    public void cacheWriteService(
            RequestDTO requestDTO) throws Exception {

        String key = requestDTO.getKey();



        try {
            DataObjectType dataObjectType =
                    requestDTO.getDataObjectType();

            DataObject dataObject =
                    dataObjectFactory.create(dataObjectType);

            dataObject.execute(requestDTO, false);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public Object cacheReadService(
            RequestDTO requestDTO) throws Exception {

        String key = requestDTO.getKey();

        ReentrantReadWriteLock lock = cacheStore.getLock(key);

        lock.readLock().lock();

        try {
            DataObjectType dataObjectType =
                    requestDTO.getDataObjectType();

            DataObject dataObject =
                    dataObjectFactory.create(dataObjectType);

            return dataObject.read(
                    key,
                    requestDTO.getPath()
            );

        } finally {
            lock.readLock().unlock();
        }
    }
}
