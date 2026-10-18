package com.distributed_cache.caching_service.controller;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadinessController {

    private final GlobalVariableClass globalVariableClass;

    public ReadinessController(GlobalVariableClass globalVariableClass) {
        this.globalVariableClass = globalVariableClass;
    }

    @GetMapping("/internal/ready")
    public ResponseEntity<String> checkReadiness() {

        if (globalVariableClass.isReady()) {
            return ResponseEntity.ok("READY");
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("NOT READY");
    }
}