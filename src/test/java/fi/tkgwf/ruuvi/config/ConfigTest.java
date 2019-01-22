package fi.tkgwf.ruuvi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void testDefaultValue() {
        assertEquals("ruuvi", Config.getInfluxUser());
    }

    @Test
    void testOverriddenStringValue() {
        assertEquals("testing", Config.getInfluxPassword());
    }

    @Test
    void testOverriddenIntegerValue() {
        assertEquals(1234, Config.getInfluxBatchMaxSize());
    }

    @Test
    void testOverriddenBooleanValue() {
        assertFalse(Config.isInfluxGzip());
    }

    @Test
    void testOverriddenDoubleValues() {
        assertEquals(Double.valueOf(0.666d), Config.getDefaultWithMotionSensitivityStrategyLowerBound());
        assertEquals(Double.valueOf(1.42d), Config.getDefaultWithMotionSensitivityStrategyUpperBound());
    }

    @Test
    void testOverriddenMacFilterList() {
        assertFalse(Config.isAllowedMAC("ABCDEF012345"));
        assertFalse(Config.isAllowedMAC("F1E2D3C4B5A6"));
        assertTrue(Config.isAllowedMAC("123000000456"));
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
