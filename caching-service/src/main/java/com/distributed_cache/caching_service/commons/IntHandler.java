
package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class IntHandler implements DataObject {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public IntHandler(
            CacheStore cacheStore,
            DatabaseService databaseService,
            ModelMapper modelMapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    private int requireInteger(Object value) {
        if (!(value instanceof Integer number)) {
            throw new IllegalArgumentException(
                    "Expected an integer value"
            );
        }

        return number;
    }

    @Override
    public Boolean execute(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        Integer value = requireInteger(requestDTO.getValue());

        return switch (requestDTO.getOperationType()) {
            case SET -> set(requestDTO, value, isSync);
            case INC -> inc(requestDTO, value, isSync);
            case DRC -> dcr(requestDTO, value, isSync);

            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };
    }

    @Override
    public Object read(String key, String path) {

        CacheValue current = cacheStore.inMemoryCache.get(key);

        return current == null
                ? null
                : current.getValue();
    }

    private CacheEventSource createEvent(
            RequestDTO requestDTO,
            int newVersion) throws Exception {

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
        event.setVersionNumber(newVersion);

        return event;
    }

    private void persistEvent(
            RequestDTO requestDTO,
            int newVersion) throws Exception {

        CacheEventSource event = createEvent(
                requestDTO,
                newVersion
        );

        databaseService.saveEventIfLeader(event);
    }

    public Boolean set(
            RequestDTO requestDTO,
            Integer value,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        int newVersion = current == null
                ? 1
                : Math.addExact(
                current.getVersionNumber(), 1
        );

        if (!Boolean.TRUE.equals(isSync)) {
            persistEvent(requestDTO, newVersion);
        }

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(value, newVersion)
        );

        return true;
    }

    public Boolean inc(
            RequestDTO requestDTO,
            Integer value,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist"
            );
        }

        if (!(current.getValue() instanceof Integer currentValue)) {
            throw new IllegalArgumentException(
                    "Existing value is not an INT"
            );
        }

        int newValue = Math.addExact(currentValue, value);

        int newVersion = Math.addExact(
                current.getVersionNumber(), 1
        );

        if (!Boolean.TRUE.equals(isSync)) {
            persistEvent(requestDTO, newVersion);
        }

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newValue, newVersion)
        );

        return true;
    }

    public Boolean dcr(
            RequestDTO requestDTO,
            Integer value,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist"
            );
        }

        if (!(current.getValue() instanceof Integer currentValue)) {
            throw new IllegalArgumentException(
                    "Existing value is not an INT"
            );
        }

        int newValue = Math.subtractExact(currentValue, value);

        int newVersion = Math.addExact(
                current.getVersionNumber(), 1
        );

        if (!Boolean.TRUE.equals(isSync)) {
            persistEvent(requestDTO, newVersion);
        }

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newValue, newVersion)
        );

        return true;
    }
}
