package com.distributed_cache.caching_service.controller;


import com.distributed_cache.caching_service.dto.GeneralResponseDTO;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.service.CacheService;
import com.distributed_cache.caching_service.service.SynchronizationService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/")
public class CacheController {

     private final CacheService cacheService;
     private final CacheStore cacheStore;
     private final SynchronizationService synchronizationService;

    public CacheController(CacheService cacheService, CacheStore cacheStore, SynchronizationService synchronizationService) {
        this.cacheService = cacheService;
        this.cacheStore = cacheStore;
        this.synchronizationService = synchronizationService;
    }

    @PostMapping("/")
    public ResponseEntity<GeneralResponseDTO> cacheWriteController(@Validated @RequestBody RequestDTO requestDTO) throws Exception {
        try{

            cacheService.cacheWriteService(requestDTO);
            return new ResponseEntity<>(new GeneralResponseDTO(
                    true
                    , "Done"
            )
            , HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>(new GeneralResponseDTO(
                    false
                    , e.toString()
            )
                    , HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/")
    public ResponseEntity<GeneralResponseDTO> cacheReadController(@Validated @RequestBody RequestDTO requestDTO) throws Exception {
        try{

            Object res = cacheService.cacheReadService(requestDTO);
            return new ResponseEntity<>(new GeneralResponseDTO(
                    true
                    , res
            )
                    , HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>(new GeneralResponseDTO(
                    false
                    , e.toString()
            )
                    , HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/outbox")
    public void sjdfnsdn() throws Exception {
        synchronizationService.outboxSync();
    }

    @PutMapping("/snapshot")
    public void jbfjkse() throws Exception {
        synchronizationService.snapshotSync();
    }

    @PutMapping("/data")
    public void sfkjsfn() throws Exception {

        for (Map.Entry<String, CacheValue> entry :
                cacheStore.inMemoryCache.entrySet()) {

            System.out.println("Key: " + entry.getKey());
            System.out.println("Value: " + entry.getValue().getValue());
            System.out.println("Version: " + entry.getValue().getVersionNumber());
        }

    }

}
