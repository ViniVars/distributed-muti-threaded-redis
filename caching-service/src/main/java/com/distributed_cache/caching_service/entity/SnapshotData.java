package com.distributed_cache.caching_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "cache_snapshot_data")
public class SnapshotData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shard_id", nullable = false)
    private Long shardId;

    @Column(name = "cache_key", nullable = false, columnDefinition = "text")
    private String cacheKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cache_value", nullable = false, columnDefinition = "jsonb")
    private Object cacheValue;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "snap_shot_version", nullable = false)
    private Long SnapshotVersion;
}