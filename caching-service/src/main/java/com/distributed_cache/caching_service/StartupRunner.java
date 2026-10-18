
package com.distributed_cache.caching_service;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.service.SynchronizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupRunner implements ApplicationRunner {

    private final SynchronizationService synchronizationService;
    private final GlobalVariableClass globalVariableClass;

    public StartupRunner(SynchronizationService synchronizationService, GlobalVariableClass globalVariableClass) {
        this.synchronizationService = synchronizationService;
        this.globalVariableClass = globalVariableClass;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        log.info("Application startup initiated. Starting cache recovery.");

        try {
            synchronizationService.snapshotSync();
            globalVariableClass.setSnapped(true);
            log.info("Cache recovery completed successfully.");

        } catch (Exception e) {
            log.error("Cache recovery failed during application startup.", e);
            throw e;
        }
    }
}
