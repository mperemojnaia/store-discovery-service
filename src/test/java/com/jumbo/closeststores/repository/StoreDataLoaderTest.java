package com.jumbo.closeststores.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.closeststores.model.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreDataLoaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StoreRepository storeRepository;

    @Captor
    private ArgumentCaptor<List<Store>> storesCaptor;

    @Test
    void shouldLoadAndPopulateRepository_withValidStoresJson() {
        StoreDataLoader loader = new StoreDataLoader(objectMapper, "stores.json", storeRepository);

        loader.loadStores();

        verify(storeRepository).populate(storesCaptor.capture());
        List<Store> loaded = storesCaptor.getValue();

        assertAll(
                () -> assertFalse(loaded.isEmpty(), "Should load at least one store"),
                () -> assertTrue(loaded.size() > 500, "Should load most stores from file")
        );
    }

    @Test
    void shouldParseLatLngAsDoubles_fromStringFields() {
        StoreDataLoader loader = new StoreDataLoader(objectMapper, "stores.json", storeRepository);

        loader.loadStores();

        verify(storeRepository).populate(storesCaptor.capture());
        Store first = storesCaptor.getValue().get(0);

        assertAll(
                () -> assertTrue(first.latitude() >= -90 && first.latitude() <= 90),
                () -> assertTrue(first.longitude() >= -180 && first.longitude() <= 180)
        );
    }

    @Test
    void shouldThrowException_whenFileIsMissing() {
        StoreDataLoader loader = new StoreDataLoader(objectMapper, "nonexistent.json", storeRepository);

        IllegalStateException ex = assertThrows(IllegalStateException.class, loader::loadStores);
        assertTrue(ex.getMessage().contains("nonexistent.json"));
    }

    @Test
    void shouldSkipStores_withInvalidCoordinates() {
        // The real stores.json has some entries with non-numeric coords
        // Loader should skip those and still load the rest
        StoreDataLoader loader = new StoreDataLoader(objectMapper, "stores.json", storeRepository);

        loader.loadStores();

        verify(storeRepository).populate(storesCaptor.capture());
        List<Store> loaded = storesCaptor.getValue();

        // All loaded stores should have valid coordinates
        assertTrue(loaded.stream().allMatch(s ->
                s.latitude() >= -90 && s.latitude() <= 90 &&
                s.longitude() >= -180 && s.longitude() <= 180
        ));
    }
}
