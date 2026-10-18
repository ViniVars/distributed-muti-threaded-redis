package com.distributed_cache.caching_service.repository;

import com.distributed_cache.caching_service.entity.SnapshotMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotMetadataRepository extends JpaRepository<SnapshotMetadata, Long> {

    Optional<SnapshotMetadata> findByShardId(Long ShardId);

}
