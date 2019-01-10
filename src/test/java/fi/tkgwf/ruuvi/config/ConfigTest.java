package fi.tkgwf.ruuvi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {
    @Test
    void testDefaultValue() {
        assertEquals("ruuvi", Config.getInfluxUser());
    }

    @Test
    void testOverriddenValue() {
        assertEquals("testing", Config.getInfluxPassword());
    }
}