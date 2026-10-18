package com.distributed_cache.caching_service.entity;


import com.distributed_cache.caching_service.utils.DataObjectType;
import com.distributed_cache.caching_service.utils.OperationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class CacheEventSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer eventSourceId;
    @NonNull
    @Enumerated(EnumType.STRING)
    private OperationType operationType;
    @NonNull
    @Enumerated(EnumType.STRING)
    private DataObjectType dataObjectType;
    @NonNull
    private String key;
    @Column(columnDefinition = "TEXT")
    private String value;
    private String path;
    @NonNull
    @CreationTimestamp
    private LocalDateTime timeStamp;

    private Integer versionNumber;
    private Boolean replicationStatus = false;
    @NonNull
    @Column(name = "shard_id")
    private Long shardID;

}
