package com.distributed_cache.caching_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class FencingToken {

    @Id
    @Column(name = "shard_id", nullable = false)
    private Long ShardID;

    @Column(name = "fencing_token_id", nullable = false)
    private Long FencingTokenID;
}