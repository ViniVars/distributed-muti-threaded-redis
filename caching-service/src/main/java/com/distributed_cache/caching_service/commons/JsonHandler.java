
package com.distributed_cache.caching_service.commons;

import com.distributed_cache.caching_service.dto.RequestDTO;
import com.distributed_cache.caching_service.entity.CacheEventSource;
import com.distributed_cache.caching_service.repository.CacheRepository;
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
    private final CacheRepository cacheRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public JsonHandler(
            CacheStore cacheStore,
            CacheRepository cacheRepository,
            ModelMapper mapper,
            ObjectMapper objectMapper) {

        this.cacheStore = cacheStore;
        this.cacheRepository = cacheRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Boolean execute(RequestDTO requestDTO) throws Exception {

        String key = requestDTO.getKey();

        synchronized (cacheStore.getLock(key)) {
            return switch (requestDTO.getOperationType()) {
                case SET -> set(requestDTO);
                default -> throw new IllegalArgumentException(
                        "Operation not supported on JSON"
                );
            };
        }
    }

    @Override
    public Object read(String key, String path) {

        synchronized (cacheStore.getLock(key)) {

            CacheValue current =
                    cacheStore.inMemoryCache.get(key);

            if (current == null) {
                return null;
            }

            JsonNode root =
                    objectMapper.valueToTree(current.getValue());

            // Empty path means read the complete JSON document.
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
    }


    public Boolean set(RequestDTO requestDTO) throws Exception {

        String key = requestDTO.getKey();
        String path = requestDTO.getPath();

        CacheValue current =
                cacheStore.inMemoryCache.get(key);

        JsonNode newRoot;

        // Empty path: replace the entire JSON document.
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

            // A nested update requires an existing JSON document.
            if (current == null) {
                throw new IllegalArgumentException(
                        "Cannot create a nested path in a missing JSON key"
                );
            }

            JsonNode oldRoot =
                    objectMapper.valueToTree(current.getValue());

            if (oldRoot == null || !oldRoot.isObject()) {
                throw new IllegalArgumentException(
                        "Path updates require a JSON object"
                );
            }

            // Never modify the currently cached object directly.
            newRoot = oldRoot.deepCopy();

            String[] parts = parsePath(path);
            ObjectNode parent = (ObjectNode) newRoot;

            // Create missing intermediate objects when necessary.
            for (int i = 0; i < parts.length - 1; i++) {

                String part = parts[i];
                JsonNode child = parent.get(part);

                if (child == null) {
                    ObjectNode newChild = objectMapper.createObjectNode();

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

            JsonNode replacement =
                    objectMapper.valueToTree(
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

        CacheEventSource event =
                mapper.map(
                        requestDTO,
                        CacheEventSource.class
                );

        event.setValue(
                objectMapper.writeValueAsString(
                        requestDTO.getValue()
                )
        );

//        event.setDataType("JSON");
        event.setReplicationStatus(false);
        event.setVersionNumber(newVersion);

        // Save the event before updating the cache.
        cacheRepository.save(event);

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
