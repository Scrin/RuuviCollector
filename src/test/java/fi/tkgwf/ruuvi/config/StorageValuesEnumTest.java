package fi.tkgwf.ruuvi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StorageValuesEnumTest {
    @Test
    void testSimpleStorageValuesEnum() {
        assertEquals(StorageValuesEnum.WHITELIST, StorageValuesEnum.resolve("whitelist"));
    }

}