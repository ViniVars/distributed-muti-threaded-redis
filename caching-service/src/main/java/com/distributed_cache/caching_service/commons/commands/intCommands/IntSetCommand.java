package com.distributed_cache.caching_service.commons.commands.intCommands;

import com.distributed_cache.caching_service.commons.commands.WriteCommand;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@Component
public class IntSetCommand implements WriteCommand {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public IntSetCommand(CacheStore cacheStore, DatabaseService databaseService, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    private CacheEventSource createEvent(
            RequestDTO requestDTO) throws Exception {
        System.out.println("Model mapper start");
        CacheEventSource event = modelMapper.map(
                requestDTO,
                CacheEventSource.class
        );
        System.out.println("Model mapper ended");

        event.setValue(
                objectMapper.writeValueAsString(
                        requestDTO.getValue()
                )
        );
        event.setReplicationStatus(false);
        return event;
    }




    @Override
    public CacheEventSource persistDB(RequestDTO requestDTO) throws Exception {

        CacheEventSource event = createEvent(requestDTO);
        return databaseService.saveEventIfLeader(event);
    }

//    @Override
//    public Boolean syncInMemory(CacheEventSource event) throws Exception {
//        String key = event.getKey();
//
//
//        ReentrantReadWriteLock lock = cacheStore.getLock(key);
//        lock.writeLock().lock();
//
//        try{
//
//            CacheValue cacheValue = cacheStore.inMemoryCache.get(key);
//
//            Integer currentVersionNumber = (cacheValue != null) ? cacheValue.getVersionNumber() : 0;
//            Integer updationValue = objectMapper.convertValue(event.getValue() , Integer.class);
//
//            if(event.getVersionNumber() > currentVersionNumber){
//                cacheStore.inMemoryCache.put(key, new CacheValue(updationValue, event.getVersionNumber()));
//
//                cacheStore.bufferClear(key, currentVersionNumber);
//                Integer maxEventSourceId = event.getEventSourceId();
//
//                while(true){
//                    event = cacheStore.bufferGetSequence(key, event.getVersionNumber());
//                    if(event == null){
//                        break;
//                    }
//
//                    executeSync(event);
//                    maxEventSourceId = max(maxEventSourceId, event.getEventSourceId());
//                }
//
//                globalVariableClass.updateLastEvent(maxEventSourceId);
//            }
//
//        }
//        finally {
//            lock.writeLock().unlock();
//        }
//
//
//        return true;
//    }


    @Override
    public Boolean syncInMemory(CacheEventSource event) {

        String key = event.getKey();
        CacheValue cacheValue = cacheStore.inMemoryCache.get(key);

        Integer currentVersionNumber = (cacheValue != null) ? cacheValue.getVersionNumber() : 0;
        Integer updationValue = objectMapper.convertValue(event.getValue() , Integer.class);

        if(event.getVersionNumber() > currentVersionNumber){
            cacheStore.inMemoryCache.put(key, new CacheValue(updationValue, event.getVersionNumber()));
            cacheStore.bufferClear(key, currentVersionNumber);
            return true;
        }
        return false;


    }
}
