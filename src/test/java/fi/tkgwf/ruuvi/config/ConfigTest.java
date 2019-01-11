package fi.tkgwf.ruuvi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigTest {
    @Test
    void testDefaultValue() {
        assertEquals("ruuvi", Config.getInfluxUser());
    }

    @Test
    void testOverriddenValue() {
        assertEquals("testing", Config.getInfluxPassword());
    }

    @Test
    void testNameThatCanBeFound() {
        assertEquals("Some named tag", Config.getTagName("AB12CD34EF56"));
    }

    @Test
    void testNameThatCanNotBeFound() {
        assertNull(Config.getTagName("123456789012"));
    }
}
