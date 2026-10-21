package com.distributed_cache.caching_service.service;


import com.distributed_cache.caching_service.commons.DataObjectFactory;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.commons.commands.WriteCommand;
import com.distributed_cache.caching_service.commons.handlers.DataObject;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import okhttp3.internal.cache.CacheStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;

@Component
public class ReplicationService {


    private final CacheStore cacheStore;
    private final DataObjectFactory dataObjectFactory;
    private final GlobalVariableClass globalVariableClass;

    public ReplicationService(CacheStore cacheStore, DataObjectFactory dataObjectFactory, GlobalVariableClass globalVariableClass) {
        this.cacheStore = cacheStore;
        this.dataObjectFactory = dataObjectFactory;
        this.globalVariableClass = globalVariableClass;
    }

    @KafkaListener(topicPattern = "cache-replication-topic")
    public void replicationListener(ConsumerRecord<String, CacheEventSource> consumerRecord, Acknowledgment acknowledgment) throws Exception {

        String key = consumerRecord.key();
        Long offset = consumerRecord.offset();
        CacheEventSource event = consumerRecord.value();

        DataObjectType dataObjectType = event.getDataObjectType();
        OperationType operationType = event.getOperationType();
        DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
        dataObject.verifyValue(event.getValue());
        WriteCommand writeCommand = dataObject.getWriteHandlerCommand(operationType);


        ReentrantReadWriteLock lock = cacheStore.getLock(key);
        lock.writeLock().lock();

        try{

            if(writeCommand.syncInMemory(event)){
                Integer maxUpdatedLastEventId = replicationBufferSyncService(event);
                globalVariableClass.updateLastEvent(maxUpdatedLastEventId);
                acknowledgment.acknowledge();
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    public Integer replicationBufferSyncService(CacheEventSource event) throws Exception {



        Integer maxEventSourceId = event.getEventSourceId();
        String key = event.getKey();

        while(true){
            event = cacheStore.bufferGetNextSequence(key, event.getVersionNumber());
            if(event == null){
                break;
            }

            DataObjectType dataObjectType = event.getDataObjectType();
            OperationType operationType = event.getOperationType();
            DataObject dataObject = dataObjectFactory.getDataHandler(dataObjectType);
            dataObject.verifyValue(event.getValue());
            WriteCommand writeCommand = dataObject.getWriteHandlerCommand(operationType);

            if(writeCommand.syncInMemory(event)){
                maxEventSourceId = max(maxEventSourceId, event.getEventSourceId());
            }
        }

        return maxEventSourceId;

    }

}
