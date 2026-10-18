package com.distributed_cache.caching_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FencingTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public FencingTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long incrementToken(Long shardId) {
        return jdbcTemplate.queryForObject("""
            UPDATE fencing_token
            SET fencing_token_id = fencing_token_id + 1
            WHERE shard_id = ?
            RETURNING fencing_token_id
            """, Long.class, shardId);
    }
}