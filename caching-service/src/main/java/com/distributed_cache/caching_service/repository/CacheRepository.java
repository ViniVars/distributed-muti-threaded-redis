package com.distributed_cache.caching_service.repository;


import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheRepository extends JpaRepository<CacheEventSource, Integer> {

}
