package com.distributed_cache.caching_service.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class CacheValue {
    private Object value;
    private Integer versionNumber;
//    private Long ttl;
//    private OffsetDateTime
}
