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
public class IntIncCommand implements WriteCommand {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public IntIncCommand(CacheStore cacheStore, DatabaseService databaseService, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
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
    public CacheEventSource persistDB(RequestDTO requestDTO) throws Exception {

        CacheEventSource event = createEvent(requestDTO);
        return databaseService.saveEventIfLeader(event);
    }

    @Override
    public Boolean syncInMemory(CacheEventSource event) {
        String key = event.getKey();
        CacheValue cacheValue = cacheStore.inMemoryCache.get(key);

        Integer currentVersionNumber = (cacheValue != null) ? cacheValue.getVersionNumber() : 0;
        Object currentValue = (cacheValue != null) ? cacheValue.getValue() : null;
        Integer cacheIntegerValue = objectMapper.convertValue(currentValue, Integer.class);
        Integer updationValue = objectMapper.convertValue(event.getValue(), Integer.class);

        if(event.getVersionNumber().equals(currentVersionNumber + 1)){
            cacheStore.inMemoryCache.put(key, new CacheValue(cacheIntegerValue + updationValue, event.getVersionNumber()));
            return true;
        }
        else if(event.getVersionNumber() > currentVersionNumber + 1){
            cacheStore.bufferKey(key, event);
        }

        return false;
    }
}
