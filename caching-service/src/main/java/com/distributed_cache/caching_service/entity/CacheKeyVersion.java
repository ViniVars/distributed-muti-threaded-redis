package com.distributed_cache.caching_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NonNull;

@Data
@Entity
public class CacheKeyVersion {

    @Id
    String key;
    @NonNull
    Integer versionNumber;
}
