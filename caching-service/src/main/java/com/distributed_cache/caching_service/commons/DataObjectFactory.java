package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.utils.DataObjectType;
import org.springframework.stereotype.Component;

@Component
public class DataObjectFactory {

    private final IntHandler intHandler;
    private final StringHandler stringHandler;
    private final ListHandler listHandler;
    private final JsonHandler jsonHandler;

    public DataObjectFactory(IntHandler intHandler, StringHandler stringHandler, ListHandler listHandler, JsonHandler jsonHandler) {
        this.intHandler = intHandler;
        this.stringHandler = stringHandler;
        this.listHandler = listHandler;
        this.jsonHandler = jsonHandler;
    }

    public DataObject create(DataObjectType dataObjectType) throws Exception {

        return switch (dataObjectType){
            case INT -> intHandler;
            case STRING -> stringHandler;
            case LIST -> listHandler;
            case JSON -> jsonHandler;
            default -> throw new Exception("Data Type Not Supported");
        };

    }
}
