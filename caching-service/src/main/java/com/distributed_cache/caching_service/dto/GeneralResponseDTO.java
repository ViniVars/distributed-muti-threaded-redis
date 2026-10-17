package com.distributed_cache.caching_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneralResponseDTO {
    private Boolean success;
    private Object message;

}
