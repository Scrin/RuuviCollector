package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DiscardUntilEnoughTimeHasElapsedStrategy;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void testDefaultStringValue() {
        assertEquals("ruuvi", Config.getInfluxUser());
    }

    @Test
    void testDefaultBooleanValue() {
        assertEquals(true, Config.isInfluxBatch());
    }

    @Test
    void testDefaultIntegerValue() {
        assertEquals(100, Config.getInfluxBatchMaxTimeMs());
    }

    @Test
    void testDefaultLongValue() {
        assertEquals(9900, Config.getMeasurementUpdateLimit());
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
        assertEquals(Double.valueOf(0.06d), Config.getDefaultWithMotionSensitivityStrategyThreshold());
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

    @Test
    void testLimitingStrategyPerMac() {
        assertTrue(Config.getLimitingStrategy("ABCDEF012345") instanceof DiscardUntilEnoughTimeHasElapsedStrategy);
        assertTrue(Config.getLimitingStrategy("F1E2D3C4B5A6") instanceof DefaultDiscardingWithMotionSensitivityStrategy);

        assertTrue(Config.getLimitingStrategy("unknown should get default") instanceof DiscardUntilEnoughTimeHasElapsedStrategy);
    }

    @Test
    void testRefreshingConfigOnTheFly() {
        // Assert the default value:
        assertEquals("ruuvi", Config.getInfluxUser());

        // Load in a new value:
        final Properties properties = new Properties();
        properties.put("influxUser", "screw");
        Config.readConfigFromProperties(properties);

        // Test that it worked:
        assertEquals("screw", Config.getInfluxUser());
    }
}
