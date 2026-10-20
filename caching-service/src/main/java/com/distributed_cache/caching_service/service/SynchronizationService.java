package com.distributed_cache.caching_service.service;


import com.distributed_cache.caching_service.commons.handlers.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.entity.SnapshotData;
import com.distributed_cache.caching_service.entity.ShardMetadata;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.repository.SnapshotDataRepository;
import com.distributed_cache.caching_service.repository.ShardMetadataRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class SynchronizationService {


    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final DataObjectFactory dataObjectFactory;
    private final ObjectMapper objectMapper;
    private final ShardMetadataRepository shardMetadataRepository;
    private final SnapshotDataRepository snapshotDataRepository;
    private final GlobalVariableClass globalVariableClass;

    public SynchronizationService(CacheStore cacheStore, CacheRepository cacheRepository, DataObjectFactory dataObjectFactory, ObjectMapper objectMapper, ShardMetadataRepository shardMetadataRepository, SnapshotDataRepository snapshotDataRepository, GlobalVariableClass globalVariableClass) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.dataObjectFactory = dataObjectFactory;
        this.objectMapper = objectMapper;
        this.shardMetadataRepository = shardMetadataRepository;
        this.snapshotDataRepository = snapshotDataRepository;
        this.globalVariableClass = globalVariableClass;
    }


    private void replayEvent(CacheEventSource cacheEvent) throws Exception {

        String key = cacheEvent.getKey();
        Integer eventVersion = cacheEvent.getVersionNumber();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        int currentVersion =
                current == null ? 0 : current.getVersionNumber();

        // Event was already applied
        if (eventVersion <= currentVersion) {
            return;
        }

        // An event is missing
        if (eventVersion != currentVersion + 1) {
            throw new IllegalStateException(
                    "Version gap for key: " + key
            );
        }



//        RequestDTO requestDTO = new RequestDTO();
//        requestDTO.setKey(key);
//        requestDTO.setOperationType(cacheEvent.getOperationType());
//        requestDTO.setPath(cacheEvent.getPath());
//        requestDTO.setValue(
//                objectMapper.readValue(
//                        cacheEvent.getValue(),
//                        Object.class
//                )
//        );

        // Apply to memory only; do not write to the database.

    }


    public void snapshotSync() throws Exception {

        Long shardId = globalVariableClass.getShardID();

        Optional<ShardMetadata> metadataOptional =
                shardMetadataRepository.findByShardId(shardId);



        Integer processedEventIdPosition = globalVariableClass.getLastEventCheckpoint();


        if (metadataOptional.isPresent() && metadataOptional.get().getLastEventIdPosition() > processedEventIdPosition) {

            // Recovery must start from a clean cache.
            cacheStore.inMemoryCache.clear();

            ShardMetadata metadata = metadataOptional.get();

            processedEventIdPosition = Math.toIntExact(metadata.getLastEventIdPosition());

            Long snapshotVersion = metadata.getLastSnapshotVersion();

            List<SnapshotData> snapshotDataList = snapshotDataRepository.getSnapshotData(shardId, snapshotVersion);

            for (SnapshotData snapshotData : snapshotDataList) {

                String key = snapshotData.getCacheKey();
                Object value = snapshotData.getCacheValue();
                int version = Math.toIntExact(snapshotData.getVersion());

                cacheStore.inMemoryCache.put(
                        key,
                        new CacheValue(value, version)
                );
            }
        }

        List<CacheEventSource> events =
                cacheRepository.getCacheEventSnapshotSync(
                        shardId, Long.valueOf(processedEventIdPosition)
                );

        for (CacheEventSource event : events) {
            DataObject dataObject = dataObjectFactory.create(event.getDataObjectType());
            dataObject.executeSync(event);
        }
    }

}
