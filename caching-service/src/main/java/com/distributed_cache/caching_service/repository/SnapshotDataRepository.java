package com.distributed_cache.caching_service.repository;

import com.distributed_cache.caching_service.entity.SnapshotData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnapshotDataRepository extends JpaRepository<SnapshotData, Long> {

    @Query(value = """
            SELECT ssd
            FROM SnapshotData ssd
            WHERE ssd.shardId = :shard_id
            AND ssd.SnapshotVersion = :snap_shot_version
            """)
    List<SnapshotData> getSnapshotData(@Param("shard_id") Long shardId,@Param("snap_shot_version") Long SnapshotVersion);
}
