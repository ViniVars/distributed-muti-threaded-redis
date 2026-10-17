
package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.distributed_cache.caching_service.utils.OperationType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ListHandler implements DataObject {

    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final ModelMapper mapper;

    public ListHandler(
            CacheStore cacheStore,
            CacheRepository cacheRepository,
            ModelMapper mapper) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.mapper = mapper;
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {
        String key = requestDTO.getKey();
        Object lockKey = cacheStore.getLock(key);

        synchronized (lockKey) {
            return switch (requestDTO.getOperationType()) {
                case APPEND -> append(requestDTO);
                case POP -> pop(requestDTO);
                default -> throw new IllegalArgumentException(
                        "Operation not supported on LIST");
            };
        }
    }

    @Override
    public Object read(String key, String path) {
        Object lockKey = cacheStore.getLock(key);

        synchronized (lockKey) {
            CacheValue current =
                    cacheStore.inMemoryCache.get(key);

            if (current == null) {
                return null;
            }

            if (!(current.getValue() instanceof List<?> list)) {
                throw new IllegalStateException(
                        "Cached value is not a LIST");
            }

            return new ArrayList<>(list);
        }
    }

    public Boolean append(RequestDTO requestDTO) {
        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        List<Object> newList = new ArrayList<>();

        int newVersion;

        if (current == null) {
            newVersion = 1;
        } else {
            if (!(current.getValue() instanceof List<?> oldList)) {
                throw new IllegalArgumentException(
                        "Existing value is not a LIST");
            }

            newList.addAll(oldList);
            newVersion = Math.addExact(
                    current.getVersionNumber(), 1);
        }

        newList.add(requestDTO.getValue());

        CacheEventSource event =
                mapper.map(requestDTO, CacheEventSource.class);

        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newList, newVersion));

        return true;
    }

    public Boolean pop(RequestDTO requestDTO) {
        String key = requestDTO.getKey();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        if (current == null) {
            throw new IllegalArgumentException(
                    "Key does not exist");
        }

        if (!(current.getValue() instanceof List<?> oldList)) {
            throw new IllegalArgumentException(
                    "Existing value is not a LIST");
        }

        if (oldList.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot POP from an empty list");
        }

        List<Object> newList = new ArrayList<>(oldList);

        // Remove the last element.
        newList.remove(newList.size() - 1);

        int newVersion = Math.addExact(
                current.getVersionNumber(), 1);

        CacheEventSource event =
                mapper.map(requestDTO, CacheEventSource.class);

        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        cacheRepository.save(event);

        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newList, newVersion));

        return true;
    }
}
