package com.jumbo.closeststores.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.closeststores.model.Position;
import com.jumbo.closeststores.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads store data from JSON on application startup and populates the repository.
 */
@Component
public class StoreDataLoader {

    private static final Logger log = LoggerFactory.getLogger(StoreDataLoader.class);

    private final ObjectMapper objectMapper;
    private final String dataFile;
    private final StoreRepository storeRepository;

    public StoreDataLoader(ObjectMapper objectMapper,
                           @Value("${store.data-file:stores.json}") String dataFile,
                           StoreRepository storeRepository) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
        this.storeRepository = storeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadStores() {
        ClassPathResource resource = new ClassPathResource(dataFile);

        if (!resource.exists()) {
            log.error("{} not found on classpath. Store data is unavailable.", dataFile);
            throw new IllegalStateException(dataFile + " not found on classpath");
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode storesNode = root.get("stores");

            if (storesNode == null || !storesNode.isArray()) {
                log.error("{} does not contain a valid 'stores' array", dataFile);
                throw new IllegalStateException(dataFile + " does not contain a 'stores' array");
            }

            List<Store> validStores = parseAndFilter(storesNode);
            storeRepository.populate(validStores);
            log.info("Loaded {} valid stores from {}", validStores.size(), dataFile);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + dataFile, e);
        }
    }

    private List<Store> parseAndFilter(JsonNode storesNode) {
        List<Store> valid = new ArrayList<>(storesNode.size());
        int skipped = 0;

        for (JsonNode node : storesNode) {
            try {
                Store store = objectMapper.treeToValue(node, Store.class);
                if (isValid(store)) {
                    valid.add(store);
                } else {
                    skipped++;
                }
            } catch (JsonProcessingException e) {
                skipped++;
                String name = node.has("addressName") ? node.get("addressName").asText() : "unknown";
                log.debug("Failed to parse store '{}': {}", name, e.getMessage());
            }
        }

        if (skipped > 0) {
            log.warn("Skipped {} invalid store entries out of {}", skipped, storesNode.size());
        }
        return valid;
    }

    private boolean isValid(Store store) {
        if (store.latitude() < Position.MIN_LATITUDE || store.latitude() > Position.MAX_LATITUDE
                || store.longitude() < Position.MIN_LONGITUDE || store.longitude() > Position.MAX_LONGITUDE) {
            log.debug("Invalid coordinates for store '{}': lat={}, lng={}",
                    store.addressName(), store.latitude(), store.longitude());
            return false;
        }
        if (store.addressName() == null || store.addressName().isBlank()) {
            log.debug("Store missing addressName, skipping");
            return false;
        }
        return true;
    }
}
