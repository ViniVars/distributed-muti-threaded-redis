
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;

import com.distributed_cache.caching_service.utils.OperationType;
import org.springframework.stereotype.Component;

@Component
public class JsonHandler implements DataObject {


    private final JsonSetCommand jsonSetCommand;
    private final JsonGetCommand jsonGetCommand;

    public JsonHandler(
            JsonSetCommand jsonSetCommand, JsonIncCommand jsonIncCommand, JsonDrcCommand jsonDrcCommand, JsonGetCommand jsonGetCommand) {
        this.jsonSetCommand = jsonSetCommand;
        this.jsonGetCommand = jsonGetCommand;
    }

    @Override
    public Boolean verifyValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Expected an not null value");
        }
        return true;
    }


    @Override
    public WriteCommand getWriteHandlerCommand(OperationType operationType){

        return switch (operationType) {
            case SET -> jsonSetCommand;
            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

    }

    @Override
    public GeneralReadCommand getReadHandlerCommand() {
        return jsonGetCommand;
    }


}
