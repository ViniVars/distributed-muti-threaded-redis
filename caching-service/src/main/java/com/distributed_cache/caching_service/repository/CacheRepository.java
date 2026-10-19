package com.distributed_cache.caching_service.repository;


import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.data.jpa.repository.JpaRepository;
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
    WHERE ces.replicationStatus = false
      AND ces.shardID = :ShardID
    ORDER BY ces.key ASC, ces.versionNumber ASC
    """)
    List<CacheEventSource> getCacheEventOutboxSync(
            @Param("ShardID") Long shardID
    );

    @Query("""
    SELECT ces
    FROM CacheEventSource ces
    WHERE ces.eventSourceId > :eventSourceId
      AND ces.shardID = :ShardID
    ORDER BY ces.key ASC, ces.versionNumber ASC
    """)
    List<CacheEventSource> getCacheEventSnapshotSync(
            @Param("ShardID") Long shardID
            , @Param("eventSourceId")Long eventSourceId
            );

}
