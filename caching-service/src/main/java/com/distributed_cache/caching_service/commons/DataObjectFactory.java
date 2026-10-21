package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.commons.handlers.*;
import com.distributed_cache.caching_service.utils.DataObjectType;
import org.springframework.stereotype.Component;

@Component
public class DataObjectFactory {

    private final IntHandler intHandler;
    private final StringHandler_old stringHandler;
    private final ListHandler_old listHandler;
    private final JsonHandler_old jsonHandler;

    public DataObjectFactory(IntHandler intHandler, StringHandler_old stringHandler, ListHandler_old listHandler, JsonHandler_old jsonHandler) {
        this.intHandler = intHandler;
        this.stringHandler = stringHandler;
        this.listHandler = listHandler;
        this.jsonHandler = jsonHandler;
    }

    public DataObject getDataHandler(DataObjectType dataObjectType) throws Exception {

        return switch (dataObjectType){
            case INT -> intHandler;
            case STRING -> stringHandler;
            case LIST -> listHandler;
            case JSON -> jsonHandler;
            default -> throw new Exception("Data Type Not Supported");
        };

    }
}
