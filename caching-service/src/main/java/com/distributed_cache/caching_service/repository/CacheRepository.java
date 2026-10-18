package com.distributed_cache.caching_service.repository;


import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CacheRepository extends JpaRepository<CacheEventSource, Integer> {

    @Query(value = """
            SELECT *
            FROM CacheEventSource ces
            WHERE ces.replicationStatus = false
            AND ces.shardID = :ShardID
            ORDER BY
                ces.key, ces.version
            """)
    List<CacheEventSource> getCacheEvent(@Param("ShardID") Long ShardID);
}
