
package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final CacheRepository cacheRepository;
    private final GlobalVariableClass globalVariableClass;

    public DatabaseService(
            JdbcTemplate jdbcTemplate,
            CacheRepository cacheRepository,
            GlobalVariableClass globalVariableClass) {

        this.jdbcTemplate = jdbcTemplate;
        this.cacheRepository = cacheRepository;
        this.globalVariableClass = globalVariableClass;
    }

    @Transactional
    public void saveEventIfLeader(CacheEventSource event) {

        // Fast local check before accessing the database.
        if (!globalVariableClass.isLeader()
                || !globalVariableClass.isReady()) {

            throw new IllegalStateException(
                    "Replica is not ready to write"
            );
        }

        Long shardId = globalVariableClass.getShardID();
        Long myToken = globalVariableClass.getFencingToken();

        if (myToken == null || myToken < 0) {
            throw new IllegalStateException(
                    "Invalid fencing token"
            );
        }

        // Lock this shard's fencing-token row.
        Long currentToken = jdbcTemplate.queryForObject(
                """
                SELECT fencing_token_id
                FROM fencing_token
                WHERE shard_id = ?
                FOR UPDATE
                """,
                Long.class,
                shardId
        );

        // Reject writes from stale leaders.
        if (currentToken == null || !currentToken.equals(myToken)) {

            globalVariableClass.setLeader(false);
            globalVariableClass.setReady(false);

            throw new IllegalStateException(
                    "Fencing token mismatch. Leadership lost."
            );
        }

        // Insert the event in the same transaction.
        cacheRepository.save(event);
    }
}
