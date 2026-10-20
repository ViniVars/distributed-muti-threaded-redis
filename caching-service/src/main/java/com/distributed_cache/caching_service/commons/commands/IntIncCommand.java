package com.distributed_cache.caching_service.commons.commands;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;


@Component
public class IntIncCommand implements WriteCommand {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final GlobalVariableClass globalVariableClass;

    public IntIncCommand(CacheStore cacheStore, DatabaseService databaseService, ModelMapper modelMapper, ObjectMapper objectMapper, GlobalVariableClass globalVariableClass) {
        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.globalVariableClass = globalVariableClass;
    }

    private CacheEventSource createEvent(
            RequestDTO requestDTO) throws Exception {

        CacheEventSource event = modelMapper.map(
                requestDTO,
                CacheEventSource.class
        );

        event.setValue(
                objectMapper.writeValueAsString(
                        requestDTO.getValue()
                )
        );

        event.setReplicationStatus(false);
        return event;
    }


    @Override
    public Boolean execute(RequestDTO requestDTO, Integer updationValue) throws Exception {
        String key = requestDTO.getKey();

        CacheEventSource event = createEvent(requestDTO);
        event = databaseService.saveEventIfLeader(event);


        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.writeLock().lock();

        try{

            CacheValue cacheValue = cacheStore.inMemoryCache.get(key);

            Integer currentVersionNumber = (cacheValue != null) ? cacheValue.getVersionNumber() : 0;
            Object currentValue = (cacheValue != null) ? cacheValue.getValue() : null;
            Integer cacheIntegerValue = objectMapper.convertValue(currentValue, Integer.class);

            if(event.getVersionNumber().equals(currentVersionNumber + 1)){
                cacheStore.inMemoryCache.put(key, new CacheValue(cacheIntegerValue + updationValue, event.getVersionNumber()));

                Integer maxEventSourceId = event.getEventSourceId();

                while(event != null){
                    event = cacheStore.bufferGetSequence(key, event.getVersionNumber());
                    executeSync(event);
                    maxEventSourceId = max(maxEventSourceId, event.getEventSourceId());
                }

                globalVariableClass.updateLastEvent(maxEventSourceId);
            }
            else if(event.getVersionNumber() <= currentVersionNumber){
                return true;
            }
            else{
                cacheStore.bufferKey(key, event);
            }

        }
        finally {
            lock.writeLock().unlock();
        }


        return true;
    }

    @Override
    public Boolean executeSync(CacheEventSource cacheEventSource) throws Exception {
        return null;
    }
}
