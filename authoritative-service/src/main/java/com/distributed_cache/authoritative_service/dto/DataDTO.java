package com.distributed_cache.authoritative_service.dto;


import lombok.Data;

@Data
public class DataDTO {

    private String key;
    private Object value;

}
