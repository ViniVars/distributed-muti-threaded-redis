package com.distributed_cache.caching_service.service;


import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.commons.commands.WriteCommand;
import com.distributed_cache.caching_service.commons.handlers.DataObject;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class ReplicationService {


    private final CacheStore cacheStore;
    private final DataObjectFactory dataObjectFactory;
    private final GlobalVariableClass globalVariableClass;
    private final CacheRepository cacheRepository;
    private final KafkaTemplate<String, CacheEventSource> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ReplicationService(CacheStore cacheStore, DataObjectFactory dataObjectFactory, GlobalVariableClass globalVariableClass, CacheRepository cacheRepository, KafkaTemplate<String, CacheEventSource> kafkaTemplate, JdbcTemplate jdbcTemplate) {
        this.cacheStore = cacheStore;
        this.dataObjectFactory = dataObjectFactory;
        this.globalVariableClass = globalVariableClass;
        this.cacheRepository = cacheRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @KafkaListener(topicPattern = "cache-replication-topic")
    public void replicationListener(ConsumerRecord<String, CacheEventSource> consumerRecord, Acknowledgment acknowledgment) throws Exception {

        String key = consumerRecord.key();
        CacheEventSource event = consumerRecord.value();

        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.writeLock().lock();

        try{

            if(replicationSync(event)){
                globalVariableClass.updateLastEvent(event.getEventSourceId());
                acknowledgment.acknowledge();
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    public Boolean replicationSync(CacheEventSource event) throws Exception {

        String key = event.getKey();

        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.writeLock().lock();

        try{
            CacheValue cacheValue = cacheStore.inMemoryCache.get(key);
            Integer currentVersion = cacheValue.getVersionNumber();
            Integer nextVersion = event.getVersionNumber();

            DataObjectType dataObjectType = event.getDataObjectType();
            OperationType operationType = event.getOperationType();
            DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
            dataObject.verifyValue(event.getValue());
            WriteCommand writeCommand = dataObject.getWriteHandlerCommand(operationType);

            if(operationType == OperationType.SET || nextVersion == currentVersion + 1){
                writeCommand.syncInMemory(event);
            }
            else{
                List<CacheEventSource> replicationGapEvents = cacheRepository.getReplicationGap(key, currentVersion, nextVersion);
                for (CacheEventSource replicationGapEvent : replicationGapEvents) {
                    writeCommand.syncInMemory(replicationGapEvent);
                }

                writeCommand.syncInMemory(event);
            }
        }
        finally {
            lock.writeLock().unlock();
        }

        return true;
    }


    @
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void replicationBackgroundWorker() {

        List<CacheEventSource> unReplicatedEvents =
                cacheRepository.nonReplicatedEvents(
                        globalVariableClass.getShardID()
                );

        if (unReplicatedEvents.isEmpty()) {
            return;
        }

        // Store the events whose Kafka sends are confirmed successful.
        List<CacheEventSource> successfullySentEvents =
                new ArrayList<>();

        // Keep the futures alongside their corresponding events.
        List<CompletableFuture<SendResult<String, CacheEventSource>>> futures =
                new ArrayList<>();

        for (CacheEventSource event : unReplicatedEvents) {
            try {
                CompletableFuture<SendResult<String, CacheEventSource>> future =
                        kafkaTemplate.send(
                                "cache-replication-topic",
                                event.getKey(),
                                event
                        );

                futures.add(future);

            } catch (Exception e) {
                // A synchronous send failure: do not mark this event replicated.
                System.err.println(
                        "Failed to submit event " +
                                event.getEventSourceId() + ": " + e.getMessage()
                );

                futures.add(CompletableFuture.failedFuture(e));
            }
        }

        // Wait for each result and collect only successful sends.
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get();

                successfullySentEvents.add(unReplicatedEvents.get(i));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Replication worker interrupted.");
                break;

            } catch (ExecutionException e) {
                System.err.println(
                        "Kafka send failed for event " +
                                unReplicatedEvents.get(i).getEventSourceId() +
                                ": " + e.getCause()
                );
            }
        }

        if (successfullySentEvents.isEmpty()) {
            return;
        }

        // Update only events whose Kafka sends were confirmed successful.
        String sql = """
            UPDATE cache_event_source
            SET replication_status = ?
            WHERE event_source_id = ?
              AND replication_status = ?
            """;

        jdbcTemplate.batchUpdate(
                sql,
                successfullySentEvents,
                100,
                (ps, event) -> {
                    ps.setBoolean(1, true);
                    ps.setLong(2, event.getEventSourceId());
                    ps.setBoolean(3, false);
                }
        );

        System.out.println(
                "Batch update completed for " +
                        successfullySentEvents.size() +
                        " successfully sent events."
        );
    }

}
