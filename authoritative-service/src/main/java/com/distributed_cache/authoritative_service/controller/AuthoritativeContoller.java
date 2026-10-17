package com.distributed_cache.authoritative_service.controller;


import com.distributed_cache.authoritative_service.dto.DataDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class AuthoritativeContoller {

    @PostMapping("/")
    @GetMapping("/")
    public ResponseEntity<Boolean> mainContoller(@RequestBody DataDTO dataDTO){
        String key = dataDTO.getKey();
        long sum = 0L;
        for(int i = 0; i < key.length(); i++){
            sum += key.charAt(i);
        }

        int res = Math.toIntExact(sum % 3);
        
    }

}
