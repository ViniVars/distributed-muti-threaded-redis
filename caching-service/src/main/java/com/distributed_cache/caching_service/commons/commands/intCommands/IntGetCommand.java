package com.distributed_cache.caching_service.commons.commands.intCommands;

import com.distributed_cache.caching_service.commons.commands.GeneralReadCommand;
import com.distributed_cache.caching_service.utils.CacheStore;
import org.springframework.stereotype.Component;

@Component
public class IntGetCommand extends GeneralReadCommand {
    public IntGetCommand(CacheStore cacheStore) {
        super(cacheStore);
    }
}
