
package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.distributed_cache.caching_service.utils.OperationType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class StringHandler implements DataObject {

    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final ModelMapper mapper;

    public StringHandler(
            CacheStore cacheStore,
            CacheRepository cacheRepository,
            ModelMapper mapper) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.mapper = mapper;
    }

    private String requireString(Object value) {
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(
                    "Expected a String value");
        }
        return string;
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {
        String key = requestDTO.getKey();
        String value = requireString(requestDTO.getValue());

        Object lockKey = cacheStore.getLock(key);

        synchronized (lockKey) {
            return switch (requestDTO.getOperationType()) {
                case SET -> set(requestDTO, value);
                default -> throw new IllegalArgumentException(
                        "Operation not supported on STRING");
            };
        }
    }

    @Override
    public Object read(String key, String path) {
        Object lockKey = cacheStore.getLock(key);

        synchronized (lockKey) {
            CacheValue current =
                    cacheStore.inMemoryCache.get(key);

            return current == null ? null : current.getValue();
        }
    }

    public Boolean set(RequestDTO requestDTO, String value) {
        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        int newVersion = current == null
                ? 1
                : Math.addExact(current.getVersionNumber(), 1);

        CacheEventSource event =
                mapper.map(requestDTO, CacheEventSource.class);

        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(value, newVersion));

        return true;
    }
}
