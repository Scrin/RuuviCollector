package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;

import java.util.Optional;

/**
 * Normally discard measurements that are coming in too fast, except when acceleration has been detected -- in that
 * case the packet is always saved.
 * <p>
 * The time limit is defined as {@link Config#getMeasurementUpdateLimit()}.
 * The acceleration bounds are defined as {@link Config#getDefaultWithMotionSensitivityStrategyLowerBound()}
 * and {@link Config#getDefaultWithMotionSensitivityStrategyUpperBound()}.
 * <p>
 * The limit is applied separately to all the different devices sending data, i.e. per MAC address.
 */
public class DefaultDiscardingWithMotionSensitivityStrategy implements LimitingStrategy {
    private final DiscardUntilEnoughTimeHasElapsedStrategy defaultStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();

    @Override
    public Optional<RuuviMeasurement> apply(final RuuviMeasurement measurement) {
        if (measurement.accelerationTotal < Config.getDefaultWithMotionSensitivityStrategyLowerBound()
            || measurement.accelerationTotal > Config.getDefaultWithMotionSensitivityStrategyUpperBound()) {
            return Optional.of(measurement);
        }
        return defaultStrategy.apply(measurement);
    }
}
