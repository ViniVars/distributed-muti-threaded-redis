
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.*;
import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
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

    private int requireInteger(Object value) {
        if (!(value instanceof Integer number)) {
            throw new IllegalArgumentException("Expected an integer value");
        }

        return number;
    }

    @Override
    public Boolean execute(RequestDTO requestDTO, Boolean isSync) throws Exception {

        Integer value = requireInteger(requestDTO.getValue());

        WriteCommand writeCommand = switch (requestDTO.getOperationType()) {
            case SET -> intSetCommand;
            case INC -> intIncCommand;
            case DRC -> intDrcCommand;

            default -> throw new IllegalArgumentException(
                    "Operation not supported on INT"
            );
        };

        return writeCommand.execute(requestDTO, value);
    }

    @Override
    public Object read(String key, String path) throws Exception {
        return generalReadCommand.execute(key, path);
    }


}
