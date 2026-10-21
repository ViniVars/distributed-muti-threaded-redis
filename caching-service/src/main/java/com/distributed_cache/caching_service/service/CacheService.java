
package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.commons.commands.GeneralReadCommand;
import com.distributed_cache.caching_service.commons.commands.WriteCommand;
import com.distributed_cache.caching_service.commons.handlers.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;

@Service
public class CacheService {

    private final DataObjectFactory dataObjectFactory;
    private final CacheStore cacheStore;
    private final GlobalVariableClass globalVariableClass;

    public CacheService(DataObjectFactory dataObjectFactory, CacheStore cacheStore, GlobalVariableClass globalVariableClass) {
        this.dataObjectFactory = dataObjectFactory;
        this.cacheStore = cacheStore;
        this.globalVariableClass = globalVariableClass;
    }

    public void cacheWriteService(RequestDTO requestDTO) throws Exception {



        DataObjectType dataObjectType = requestDTO.getDataObjectType();
        OperationType operationType = requestDTO.getOperationType();
        DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
        dataObject.verifyValue(requestDTO.getValue());
        WriteCommand writeCommand = dataObject.getWriteHandlerCommand(operationType);

        CacheEventSource savedEvent = writeCommand.persistDB(requestDTO);

        String key = savedEvent.getKey();
        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.writeLock().lock();

        try{

            if(writeCommand.syncInMemory(savedEvent)){
                Integer maxUpdatedLastEventId = cacheBufferSyncService(savedEvent);
                globalVariableClass.updateLastEvent(maxUpdatedLastEventId);
            }
        }
        finally {
            lock.writeLock().unlock();
        }

    }

    public Object cacheReadService(RequestDTO requestDTO) throws Exception {

            String key = requestDTO.getKey();
            DataObjectType dataObjectType = requestDTO.getDataObjectType();
            DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
            GeneralReadCommand generalReadCommand = dataObject.getReadHandlerCommand();
            return generalReadCommand.read(key, requestDTO.getPath());
    }

    public Integer cacheBufferSyncService(CacheEventSource event) throws Exception {



        Integer maxEventSourceId = event.getEventSourceId();
        String key = event.getKey();

        while(true){
            event = cacheStore.bufferGetNextSequence(key, event.getVersionNumber());
            if(event == null){
                break;
            }

            DataObjectType dataObjectType = event.getDataObjectType();
            OperationType operationType = event.getOperationType();
            DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
            dataObject.verifyValue(event.getValue());
            WriteCommand writeCommand = dataObject.getWriteHandlerCommand(operationType);

            if(writeCommand.syncInMemory(event)){
                maxEventSourceId = max(maxEventSourceId, event.getEventSourceId());
            }
        }

        return maxEventSourceId;

    }
}
