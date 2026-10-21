
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;

import com.distributed_cache.caching_service.utils.OperationType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListHandler implements DataObject {


    private final ListSetCommand listSetCommand;
    private final ListAppendCommand listAppendCommand;
    private final ListPopCommand listPopCommand;
    private final ListGetCommand listGetCommand;



    @Override
    public Boolean verifyValue(Object value) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Expected an Integer value");
        }
        return true;
    }


    @Override
    public WriteCommand getWriteHandlerCommand(OperationType operationType){

        return switch (operationType) {
            case SET -> listSetCommand;
            case APPEND -> listAppendCommand;
            case POP -> listPopCommand;
            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

    }

    @Override
    public GeneralReadCommand getReadHandlerCommand() {
        return listGetCommand;
    }


}
