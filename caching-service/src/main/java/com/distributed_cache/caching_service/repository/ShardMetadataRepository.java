package com.distributed_cache.caching_service.repository;

import com.distributed_cache.caching_service.entity.ShardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShardMetadataRepository extends JpaRepository<ShardMetadata, Long> {

    Optional<ShardMetadata> findByShardId(Long ShardId);

}
