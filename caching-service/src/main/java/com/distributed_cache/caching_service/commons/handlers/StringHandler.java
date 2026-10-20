
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class StringHandler implements DataObject {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public StringHandler(
            CacheStore cacheStore,
            DatabaseService databaseService,
            ModelMapper mapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    private String requireString(Object value) {
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(
                    "Expected a String value"
            );
        }

        return string;
    }

    @Override
    public Boolean execute(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        String value = requireString(requestDTO.getValue());

        return switch (requestDTO.getOperationType()) {
            case SET -> set(requestDTO, value, isSync);

            default -> throw new IllegalArgumentException(
                    "Operation not supported on STRING"
            );
        };
    }

    @Override
    public Object read(String key, String path) {

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        return current == null
                ? null
                : current.getValue();
    }

    public Boolean set(
            RequestDTO requestDTO,
            String value,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        int newVersion = current == null
                ? 1
                : Math.addExact(
                current.getVersionNumber(), 1
        );

        // Normal writes use the transactional database service.
        if (!Boolean.TRUE.equals(isSync)) {

            CacheEventSource event =
                    mapper.map(
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

            // Validate fencing and save the event transactionally.
            databaseService.saveEventIfLeader(event);
        }

        // Update memory after persistence succeeds,
        // or immediately during synchronization.
        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(value, newVersion)
        );

        return true;
    }
}
