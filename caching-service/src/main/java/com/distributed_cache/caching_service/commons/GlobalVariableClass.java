package com.distributed_cache.caching_service.commons;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Data
public class GlobalVariableClass {
    public volatile boolean isLeader = false;
    public volatile boolean isReady = false;
    public String InstanceID = UUID.randomUUID().toString();
    public volatile Long FencingToken = -99L;
    public volatile Long ShardID = 0L;
}
