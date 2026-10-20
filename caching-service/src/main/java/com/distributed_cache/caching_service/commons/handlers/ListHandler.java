
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ListHandler implements DataObject {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public ListHandler(
            CacheStore cacheStore,
            DatabaseService databaseService,
            ModelMapper mapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }


    public Boolean execute(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        return switch (requestDTO.getOperationType()) {
            case APPEND -> append(requestDTO, isSync);
            case POP -> pop(requestDTO, isSync);

            default -> throw new IllegalArgumentException(
                    "Operation not supported on LIST"
            );
        };
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {
        return null;
    }

    @Override
    public Boolean executeSync(CacheEventSource cacheEventSource) throws Exception {
        return null;
    }

    @Override
    public Object read(String key, String path) {

        CacheValue current = cacheStore.inMemoryCache.get(key);

        if (current == null) {
            return null;
        }

        if (!(current.getValue() instanceof List<?> list)) {
            throw new IllegalStateException(
                    "Cached value is not a LIST"
            );
        }

        // Return a copy so callers cannot modify cached state.
        return new ArrayList<>(list);
    }

    private void persistEvent(
            RequestDTO requestDTO,
            int newVersion) throws Exception {

        CacheEventSource event = mapper.map(
                requestDTO,
                CacheEventSource.class
        );

        // Serialize the value so the event can be replayed.
        event.setValue(
                objectMapper.writeValueAsString(
                        requestDTO.getValue()
                )
        );

        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        // Validate leadership and persist the event.
        databaseService.saveEventIfLeader(event);
    }

    public Boolean append(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        List<Object> newList = new ArrayList<>();

        int newVersion;

        if (current == null) {
            newVersion = 1;
        } else {

            if (!(current.getValue() instanceof List<?> oldList)) {
                throw new IllegalArgumentException(
                        "Existing value is not a LIST"
                );
            }

            newList.addAll(oldList);

            newVersion = Math.addExact(
                    current.getVersionNumber(), 1
            );
        }

        newList.add(requestDTO.getValue());

        // Normal writes persist before updating memory.
        // Synchronization writes skip database persistence.
        if (!Boolean.TRUE.equals(isSync)) {
            persistEvent(requestDTO, newVersion);
        }

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newList, newVersion)
        );

        return true;
    }

    public Boolean pop(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist"
            );
        }

        if (!(current.getValue() instanceof List<?> oldList)) {
            throw new IllegalArgumentException(
                    "Existing value is not a LIST"
            );
        }

        if (oldList.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot POP from an empty list"
            );
        }

        List<Object> newList = new ArrayList<>(oldList);

        // Remove the last element.
        newList.remove(newList.size() - 1);

        int newVersion = Math.addExact(
                current.getVersionNumber(), 1
        );

        if (!Boolean.TRUE.equals(isSync)) {
            persistEvent(requestDTO, newVersion);
        }

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newList, newVersion)
        );

        return true;
    }
}
