
package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.repository.FencingTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    private final FencingTokenRepository fencingTokenRepository;
    private final CacheRepository cacheRepository;
    private final GlobalVariableClass globalVariableClass;

    public DatabaseService(FencingTokenRepository fencingTokenRepository, CacheRepository cacheRepository,GlobalVariableClass globalVariableClass) {
        this.fencingTokenRepository = fencingTokenRepository;
        this.cacheRepository = cacheRepository;
        this.globalVariableClass = globalVariableClass;
    }

    @Transactional
    public CacheEventSource saveEventIfLeader(CacheEventSource event) {

        // Fast local check before accessing the database.
        if (!globalVariableClass.isLeader() || !globalVariableClass.isReady()) {

            throw new IllegalStateException("Replica is not ready to write");
        }

        Long shardId = globalVariableClass.getShardID();
        Long myToken = globalVariableClass.getFencingToken();

        if (myToken == null || myToken < 0) {
            throw new IllegalStateException("Invalid fencing token");
        }

        // Lock this shard's fencing-token row.
        Long currentToken = fencingTokenRepository.readToken(shardId);

        // Reject writes from stale leaders.
        if (currentToken == null || !currentToken.equals(myToken)) {

            globalVariableClass.setLeader(false);
            globalVariableClass.setReady(false);

            throw new IllegalStateException("Fencing token mismatch. Leadership lost.");
        }

        // Insert the event in the same transaction.
        return cacheRepository.save(event);
//        globalVariableClass.setLastEventCheckpoint(globalVariableClass.getLastEventCheckpoint() + 1);
    }
}
