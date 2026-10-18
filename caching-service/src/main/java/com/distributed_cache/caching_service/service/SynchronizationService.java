package com.distributed_cache.caching_service.service;


import com.distributed_cache.caching_service.commons.DataObject;
import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.entity.SnapshotData;
import com.distributed_cache.caching_service.entity.SnapshotMetadata;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.repository.SnapshotDataRepository;
import com.distributed_cache.caching_service.repository.SnapshotMetadataRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SynchronizationService {


    private final CacheStore cacheStore;
    private final CacheRepository cacheRepository;
    private final DataObjectFactory dataObjectFactory;
    private final ObjectMapper objectMapper;
    private final SnapshotMetadataRepository snapshotMetadataRepository;
    private final SnapshotDataRepository snapshotDataRepository;
    private final GlobalVariableClass globalVariableClass;

    public SynchronizationService(CacheStore cacheStore, CacheRepository cacheRepository, DataObjectFactory dataObjectFactory, ObjectMapper objectMapper, SnapshotMetadataRepository snapshotMetadataRepository, SnapshotDataRepository snapshotDataRepository, GlobalVariableClass globalVariableClass) {
        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.dataObjectFactory = dataObjectFactory;
        this.objectMapper = objectMapper;
        this.snapshotMetadataRepository = snapshotMetadataRepository;
        this.snapshotDataRepository = snapshotDataRepository;
        this.globalVariableClass = globalVariableClass;
    }


    private void replayEvent(CacheEventSource cacheEvent) throws Exception {

        String key = cacheEvent.getKey();
        Integer eventVersion = cacheEvent.getVersionNumber();

        CacheValue current = cacheStore.inMemoryCache.get(key);
//        System.out.println(cacheEvent + "\n");
//        System.out.println(current + "\n\n");

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

        DataObject dataObject =
                dataObjectFactory.create(cacheEvent.getDataObjectType());

        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setKey(key);
        requestDTO.setOperationType(cacheEvent.getOperationType());
        requestDTO.setPath(cacheEvent.getPath());
        requestDTO.setValue(
                objectMapper.readValue(
                        cacheEvent.getValue(),
                        Object.class
                )
        );

        // Apply to memory only; do not write to the database.
        dataObject.execute(requestDTO, true);
    }


    public void snapshotSync() throws Exception {

        Long shardId = globalVariableClass.getShardID();

        Optional<SnapshotMetadata> metadataOptional =
                snapshotMetadataRepository.findByShardId(shardId);

        // Recovery must start from a clean cache.
        cacheStore.inMemoryCache.clear();

        OffsetDateTime lastProcessedTimeStamp =
                OffsetDateTime.of(
                        1900, 1, 1,
                        0, 0, 0, 0,
                        ZoneOffset.UTC
                );

        if (metadataOptional.isPresent()) {

            SnapshotMetadata metadata = metadataOptional.get();

            lastProcessedTimeStamp = metadata.getCreatedAt();

            Long snapshotVersion = metadata.getLastEventPosition();

            List<SnapshotData> snapshotDataList =
                    snapshotDataRepository.getSnapshotData(
                            shardId, snapshotVersion
                    );

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
                        shardId, lastProcessedTimeStamp
                );

        for (CacheEventSource event : events) {
            replayEvent(event);
        }
    }

    public void outboxSync() throws Exception {

//        List<CacheEventSource> cacheEventSourceList = cacheRepository.getCacheEventOutboxSync(globalVariableClass.getShardID());
//
//
//        for (CacheEventSource cacheEvent : cacheEventSourceList) {
//            replayEvent(cacheEvent);
//        }

        for (Map.Entry<String, CacheValue> entry :
                cacheStore.inMemoryCache.entrySet()) {

            System.out.println("Key: " + entry.getKey());
            System.out.println("Value: " + entry.getValue().getValue());
            System.out.println("Version: " + entry.getValue().getVersionNumber());
        }

    }


}
