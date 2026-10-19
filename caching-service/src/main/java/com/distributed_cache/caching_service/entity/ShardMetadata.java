package com.distributed_cache.caching_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "cache_snapshot_metadata")
public class ShardMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "shard_id", nullable = false)
    private Long shardId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "last_snap_shot_version", nullable = false)
    private Long lastSnapshotVersion;

    @Column(name = "last_event_id_position", nullable = false)
    private Long lastEventIdPosition;
}