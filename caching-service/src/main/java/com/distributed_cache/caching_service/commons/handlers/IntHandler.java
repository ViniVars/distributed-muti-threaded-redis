
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;
import com.distributed_cache.caching_service.dto.RequestDTO;

import com.distributed_cache.caching_service.entity.CacheEventSource;
import org.springframework.stereotype.Component;

@Component
public class IntHandler implements DataObject {


    private final IntSetCommand intSetCommand;
    private final IntIncCommand intIncCommand;
    private final IntDrcCommand intDrcCommand;
    private final GeneralReadCommand generalReadCommand;

    public IntHandler(
            IntSetCommand intSetCommand, IntIncCommand intIncCommand, IntDrcCommand intDrcCommand, GeneralReadCommand generalReadCommand) {
        this.intSetCommand = intSetCommand;
        this.intIncCommand = intIncCommand;
        this.intDrcCommand = intDrcCommand;
        this.generalReadCommand = generalReadCommand;
    }

    private void requireInteger(Object value) {
        if (!(value instanceof Integer number)) {
            throw new IllegalArgumentException("Expected an integer value");
        }
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {

        requireInteger(requestDTO.getValue());

        WriteCommand writeCommand = switch (requestDTO.getOperationType()) {
            case SET -> intSetCommand;
            case INC -> intIncCommand;
            case DRC -> intDrcCommand;

            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

        return writeCommand.execute(requestDTO);
    }

    public Boolean executeSync(CacheEventSource cacheEventSource) throws Exception {

        requireInteger(cacheEventSource.getValue());

        WriteCommand writeCommand = switch (cacheEventSource.getOperationType()) {
            case SET -> intSetCommand;
            case INC -> intIncCommand;
            case DRC -> intDrcCommand;

            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

        return writeCommand.executeSync(cacheEventSource);
    }

    @Override
    public Object read(String key, String path) throws Exception {
        return generalReadCommand.execute(key, path);
    }


}
