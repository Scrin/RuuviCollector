package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.config.ConfigTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDiscardingWithMotionSensitivityStrategyTest {

    @BeforeEach
    void resetConfigBefore() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @AfterAll
    static void resetConfigAfter() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @Test
    void testMotionSensitivity() {
        final DefaultDiscardingWithMotionSensitivityStrategy strategy = new DefaultDiscardingWithMotionSensitivityStrategy();

        final EnhancedRuuviMeasurement withinInterval = new EnhancedRuuviMeasurement();
        withinInterval.setAccelerationX(0.98d);

        final EnhancedRuuviMeasurement belowLower = new EnhancedRuuviMeasurement();
        belowLower.setAccelerationX(0.5d);


        final EnhancedRuuviMeasurement aboveUpper = new EnhancedRuuviMeasurement();
        aboveUpper.setAccelerationX(2.5d);

        assertTrue(strategy.apply(withinInterval).isPresent()); // Because no previous measurements
        assertFalse(strategy.apply(withinInterval).isPresent()); // Because of recent measurement
        assertFalse(strategy.apply(withinInterval).isPresent()); // Because of recent measurement

        assertTrue(strategy.apply(belowLower).isPresent()); // Because it's below the lower limit
        assertTrue(strategy.apply(belowLower).isPresent()); // Because it's not changing but happens right after a change
        assertFalse(strategy.apply(belowLower).isPresent()); // Because it's not changing. It's low, but not changing.

        assertTrue(strategy.apply(withinInterval).isPresent()); // Because it's changing.

        assertTrue(strategy.apply(aboveUpper).isPresent()); // Because it's changing.
        assertTrue(strategy.apply(aboveUpper).isPresent()); // Because not changing but happens right after a change
        assertFalse(strategy.apply(aboveUpper).isPresent()); // Because it's not changing. It's high, but not changing
    }
}
