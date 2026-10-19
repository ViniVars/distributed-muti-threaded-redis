package com.distributed_cache.caching_service.dto;


import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class RequestDTO {

    @NonNull
    private OperationType operationType;
    @NonNull
    private DataObjectType dataObjectType;
    @NonNull
    private String key;
    @NonNull
    private Object value;
    private String path;

    @NonNull
    private OffsetDateTime timeStamp;

    @NonNull
    private Long ttl;
    private Long shardID = 0L;

}
