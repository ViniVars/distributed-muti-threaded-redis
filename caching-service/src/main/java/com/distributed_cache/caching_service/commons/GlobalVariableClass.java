package com.distributed_cache.caching_service.commons;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Data
public class GlobalVariableClass {

    public volatile boolean isLeader = false;
    public volatile boolean isReady = false;
    public volatile boolean isSnapped = false;
    public String InstanceID = UUID.randomUUID().toString();
    public volatile Long FencingToken = -99L;
    public volatile Long ShardID = 0L;
    public volatile Integer lastEventCheckpoint = 0;


    public final Object lastEventLock = new Object();


    public void updateLastEvent(Integer value){

        synchronized (lastEventLock){
            if(lastEventCheckpoint < value){
                lastEventCheckpoint = value;
            }
        }
    }

}
