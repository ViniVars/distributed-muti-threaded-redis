
package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class IntHandler implements DataObject {

    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public IntHandler(
            CacheStore cacheStore,
            CacheRepository cacheRepository,
            ModelMapper modelMapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
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
    public Boolean execute(RequestDTO requestDTO) throws Exception {

        String key = requestDTO.getKey();
        Integer value = requireInteger(requestDTO.getValue());

        synchronized (cacheStore.getLock(key)) {

            return switch (requestDTO.getOperationType()) {
                case SET -> set(requestDTO, value);
                case INC -> inc(requestDTO, value);
                case DRC -> dcr(requestDTO, value);
                default -> throw new IllegalArgumentException(
                        "Operation not supported on INT"
                );
            };
        }
    }

    @Override
    public Object read(String key, String path) throws Exception {

        synchronized (cacheStore.getLock(key)) {

            CacheValue current =
                    cacheStore.inMemoryCache.get(key);

            return current == null ? null : current.getValue();
        }
    }

    private CacheEventSource createEvent(
            RequestDTO requestDTO,
            int newVersion) throws Exception {

        CacheEventSource event =
                modelMapper.map(
                        requestDTO,
                        CacheEventSource.class
                );

        // Store a JSON-serialized value as TEXT in PostgreSQL.
        String serializedValue =
                objectMapper.writeValueAsString(
                        requestDTO.getValue()
                );

        event.setValue(serializedValue);
        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        return event;
    }

    public Boolean set(
            RequestDTO requestDTO,
            Integer value) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        int newVersion = current == null
                ? 1
                : Math.addExact(current.getVersionNumber(), 1);

        CacheEventSource event =
                createEvent(requestDTO, newVersion);

        // Persist the event before changing the in-memory cache.
        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(value, newVersion)
        );

        return true;
    }

    public Boolean inc(
            RequestDTO requestDTO,
            Integer value) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist"
            );
        }

        Integer currentValue = (Integer) current.getValue();

        int newValue = Math.addExact(currentValue, value);
        int newVersion =
                Math.addExact(current.getVersionNumber(), 1);

        CacheEventSource event =
                createEvent(requestDTO, newVersion);

        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newValue, newVersion)
        );

        return true;
    }

    public Boolean dcr(
            RequestDTO requestDTO,
            Integer value) throws Exception {

        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist"
            );
        }

        Integer currentValue = (Integer) current.getValue();

        int newValue = Math.subtractExact(currentValue, value);
        int newVersion =
                Math.addExact(current.getVersionNumber(), 1);

        CacheEventSource event =
                createEvent(requestDTO, newVersion);

        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newValue, newVersion)
        );

        return true;
    }
}
