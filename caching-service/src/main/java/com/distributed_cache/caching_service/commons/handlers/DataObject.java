package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.commons.commands.GeneralReadCommand;
import com.distributed_cache.caching_service.commons.commands.WriteCommand;
import com.distributed_cache.caching_service.utils.OperationType;

public interface DataObject {

    WriteCommand getWriteHandlerCommand(OperationType operationType);
    GeneralReadCommand getReadHandlerCommand();
    Boolean verifyValue(Object value);
}
