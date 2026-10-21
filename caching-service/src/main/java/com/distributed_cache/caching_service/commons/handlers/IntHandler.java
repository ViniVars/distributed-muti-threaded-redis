
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;

import com.distributed_cache.caching_service.commons.commands.intCommands.IntDrcCommand;
import com.distributed_cache.caching_service.commons.commands.intCommands.IntGetCommand;
import com.distributed_cache.caching_service.commons.commands.intCommands.IntIncCommand;
import com.distributed_cache.caching_service.commons.commands.intCommands.IntSetCommand;
import com.distributed_cache.caching_service.utils.OperationType;
import org.springframework.stereotype.Component;

@Component
public class IntHandler implements DataObject {


    private final IntSetCommand intSetCommand;
    private final IntIncCommand intIncCommand;
    private final IntDrcCommand intDrcCommand;
    private final IntGetCommand intGetCommand;

    public IntHandler(
            IntSetCommand intSetCommand, IntIncCommand intIncCommand, IntDrcCommand intDrcCommand, IntGetCommand intGetCommand) {
        this.intSetCommand = intSetCommand;
        this.intIncCommand = intIncCommand;
        this.intDrcCommand = intDrcCommand;
        this.intGetCommand = intGetCommand;
    }

    @Override
    public Boolean verifyValue(Object value) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Expected an integer value");
        }
        return true;
    }


    @Override
    public WriteCommand getWriteHandlerCommand(OperationType operationType){

        return switch (operationType) {
            case SET -> intSetCommand;
            case INC -> intIncCommand;
            case DRC -> intDrcCommand;
            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

    }

    @Override
    public GeneralReadCommand getReadHandlerCommand() {
        return intGetCommand;
    }


}
