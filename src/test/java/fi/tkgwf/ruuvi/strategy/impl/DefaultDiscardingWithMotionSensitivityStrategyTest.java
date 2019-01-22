package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDiscardingWithMotionSensitivityStrategyTest {
    @Test
    void testMotionSensitivity() {
        final DefaultDiscardingWithMotionSensitivityStrategy strategy = new DefaultDiscardingWithMotionSensitivityStrategy();

        final RuuviMeasurement withinInterval = new RuuviMeasurement();
        withinInterval.accelerationTotal = 0.98d;

        final RuuviMeasurement belowLower = new RuuviMeasurement();
        belowLower.accelerationTotal = 0.5d;


        final RuuviMeasurement aboveUpper = new RuuviMeasurement();
        aboveUpper.accelerationTotal = 2.5d;

        assertTrue(strategy.apply(withinInterval).isPresent()); // Because no previous measurements
        assertFalse(strategy.apply(withinInterval).isPresent()); // Because of recent measurement
        assertFalse(strategy.apply(withinInterval).isPresent()); // Because of recent measurement

        assertTrue(strategy.apply(belowLower).isPresent()); // Because it's below the lower limit
        assertTrue(strategy.apply(belowLower).isPresent()); // Because it's below the lower limit
        assertTrue(strategy.apply(belowLower).isPresent()); // Because it's below the lower limit

        assertFalse(strategy.apply(withinInterval).isPresent()); // Because of recent measurement

        assertTrue(strategy.apply(aboveUpper).isPresent()); // Because it's above the upper limit
        assertTrue(strategy.apply(aboveUpper).isPresent()); // Because it's above the upper limit
        assertTrue(strategy.apply(aboveUpper).isPresent()); // Because it's above the upper limit
    }
}
