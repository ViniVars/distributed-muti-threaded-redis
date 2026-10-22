package com.distributed_cache.caching_service.repository;


import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CacheRepository extends JpaRepository<CacheEventSource, Integer> {


    @Query("""
    SELECT ces
    FROM CacheEventSource ces
    WHERE ces.eventSourceId > :eventSourceId
      AND ces.shardID = :ShardID
    ORDER BY ces.versionNumber ASC
    """)
    List<CacheEventSource> getCacheEventSnapshotSync(
            @Param("ShardID") Long shardID
            , @Param("eventSourceId")Long eventSourceId
            );



    @Query(value = """
            SELECT ces
            FROM CacheEventSource ces
            WHERE ces.key = :key
            AND ces.versionNumber > :lower_bound
            AND ces.versionNumber < :upper_bound
            AND ces.shardID = :shard_id
            ORDER BY ces.eventSourceId ASC
            """)
    List<CacheEventSource> getReplicationGap(@Param("key") String key, @Param("lower_bound") Integer lowerBound, @Param("upper_bound") Integer upperBound, @Param("shard_id") Integer ShardId);


    @Query("""
    SELECT ces
    FROM CacheEventSource ces
    WHERE ces.replicationStatus = false
      AND ces.shardID = :ShardID
    ORDER BY ces.eventSourceId ASC
    """)
    List<CacheEventSource> nonReplicatedEvents(
            @Param("ShardID") Long shardID
    );

}
