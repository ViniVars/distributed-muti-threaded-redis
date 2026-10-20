
package com.distributed_cache.caching_service.commons.handlers;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.service.DatabaseService;
import com.distributed_cache.caching_service.utils.CacheStore;
import com.distributed_cache.caching_service.utils.CacheValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonHandler implements DataObject {

    private final CacheStore cacheStore;
    private final DatabaseService databaseService;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public JsonHandler(
            CacheStore cacheStore,
            DatabaseService databaseService,
            ModelMapper mapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.databaseService = databaseService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }


    public Boolean execute(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        return switch (requestDTO.getOperationType()) {
            case SET -> set(requestDTO, isSync);

            default -> throw new IllegalArgumentException(
                    "Operation not supported on JSON"
            );
        };
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {
        return null;
    }

    @Override
    public Boolean executeSync(CacheEventSource cacheEventSource) throws Exception {
        return null;
    }

    @Override
    public Object read(String key, String path) {

        CacheValue current = cacheStore.inMemoryCache.get(key);

        if (current == null) {
            return null;
        }

        JsonNode root = objectMapper.valueToTree(
                current.getValue()
        );

        // Empty path reads the complete JSON document.
        if (path == null || path.isBlank()) {
            return objectMapper.convertValue(
                    root,
                    Object.class
            );
        }

        String[] parts = parsePath(path);
        JsonNode node = root;

        for (String part : parts) {

            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException(
                        "Path does not match the JSON structure: "
                                + path
                );
            }

            node = node.get(part);

            if (node == null) {
                throw new IllegalArgumentException(
                        "JSON field does not exist: " + part
                );
            }
        }

        return objectMapper.convertValue(
                node,
                Object.class
        );
    }

    public Boolean set(
            RequestDTO requestDTO,
            Boolean isSync) throws Exception {

        String key = requestDTO.getKey();
        String path = requestDTO.getPath();

        CacheValue current = cacheStore.inMemoryCache.get(key);

        JsonNode newRoot;

        // Empty path replaces the complete JSON document.
        if (path == null || path.isBlank()) {

            newRoot = objectMapper.valueToTree(
                    requestDTO.getValue()
            );

            if (newRoot == null || newRoot.isNull()) {
                throw new IllegalArgumentException(
                        "JSON value cannot be null"
                );
            }

        } else {

            // Nested updates require an existing JSON document.
            if (current == null) {
                throw new IllegalArgumentException(
                        "Cannot create a nested path in a missing JSON key"
                );
            }

            JsonNode oldRoot = objectMapper.valueToTree(
                    current.getValue()
            );

            if (oldRoot == null || !oldRoot.isObject()) {
                throw new IllegalArgumentException(
                        "Path updates require a JSON object"
                );
            }

            // Never mutate the cached JSON document directly.
            newRoot = oldRoot.deepCopy();

            String[] parts = parsePath(path);
            ObjectNode parent = (ObjectNode) newRoot;

            // Create missing intermediate objects.
            for (int i = 0; i < parts.length - 1; i++) {

                String part = parts[i];
                JsonNode child = parent.get(part);

                if (child == null) {

                    ObjectNode newChild =
                            objectMapper.createObjectNode();

                    parent.set(part, newChild);
                    parent = newChild;

                } else if (child.isObject()) {

                    parent = (ObjectNode) child;

                } else {

                    throw new IllegalArgumentException(
                            "Cannot create path '" + path
                                    + "': field '" + part
                                    + "' exists but is not a JSON object"
                    );
                }
            }

            // Create or replace the final field.
            String targetField = parts[parts.length - 1];

            JsonNode replacement = objectMapper.valueToTree(
                    requestDTO.getValue()
            );

            if (replacement == null || replacement.isNull()) {
                throw new IllegalArgumentException(
                        "JSON value cannot be null"
                );
            }

            parent.set(targetField, replacement);
        }

        int newVersion = current == null
                ? 1
                : Math.addExact(
                current.getVersionNumber(),
                1
        );

        // Normal writes persist before updating memory.
        if (!Boolean.TRUE.equals(isSync)) {

            CacheEventSource event = mapper.map(
                    requestDTO,
                    CacheEventSource.class
            );

            // Preserve the requested value and path for replay.
            event.setValue(
                    objectMapper.writeValueAsString(
                            requestDTO.getValue()
                    )
            );

            event.setReplicationStatus(false);
            event.setVersionNumber(newVersion);

            databaseService.saveEventIfLeader(event);
        }

        // Update memory only after successful persistence.
        cacheStore.inMemoryCache.put(
                key,
                new CacheValue(newRoot, newVersion)
        );

        return true;
    }

    private String[] parsePath(String path) {

        String[] parts = path.split("\\.", -1);

        for (String part : parts) {

            if (part.isBlank()
                    || part.contains("[")
                    || part.contains("]")
                    || part.contains("$")) {

                throw new IllegalArgumentException(
                        "Invalid JSON path: " + path
                );
            }
        }

        return parts;
    }
}
