package com.distributed_cache.caching_service.controller;


import com.distributed_cache.caching_service.dto.GeneralResponseDTO;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.service.CacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class CacheController {

     private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
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

}
