
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;

import com.distributed_cache.caching_service.utils.OperationType;
import org.springframework.stereotype.Component;

@Component
public class StringHandler implements DataObject {


    private final StringSetCommand stringSetCommand;
    private final StringGetCommand stringGetCommand;

    public StringHandler(
            StringSetCommand stringSetCommand, StringIncCommand stringIncCommand, StringDrcCommand stringDrcCommand, StringGetCommand stringGetCommand) {
        this.stringSetCommand = stringSetCommand;
        this.stringGetCommand = stringGetCommand;
    }

    @Override
    public Boolean verifyValue(Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected an stringeger value");
        }
        return true;
    }


    @Override
    public WriteCommand getWriteHandlerCommand(OperationType operationType){

        return switch (operationType) {
            case SET -> stringSetCommand;
            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

    }

    @Override
    public GeneralReadCommand getReadHandlerCommand() {
        return stringGetCommand;
    }


}
