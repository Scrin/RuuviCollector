package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Normally discard measurements that are coming in too fast, except when a sudden acceleration change
 * takes place -- in that case the measurement is always saved.
 * </p><p>
 * The time limit is defined as {@link Config#getMeasurementUpdateLimit()}.
 * The acceleration bounds are defined as a percentage value compared to the previous measurement
 * with {@link Config#getDefaultWithMotionSensitivityStrategyThresholdPercentage()}.
 * </p><p>
 * The limit is applied separately to all the different devices sending data, i.e. per MAC address.
 * </p>
 */
public class DefaultDiscardingWithMotionSensitivityStrategy implements LimitingStrategy {
    private final DiscardUntilEnoughTimeHasElapsedStrategy defaultStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();

    private final Double threshold = Config.getDefaultWithMotionSensitivityStrategyThresholdPercentage();
    private final Map<String, List<RuuviMeasurement>> previousMeasurementsPerMac = new HashMap<>();
    private final Map<String, Boolean> previousOutsideOfRangePerMac = new HashMap<>();

    @Override
    public Optional<RuuviMeasurement> apply(final RuuviMeasurement measurement) {
        final List<RuuviMeasurement> previousMeasurements = previousMeasurementsPerMac.getOrDefault(measurement.mac, new LinkedList<>());
        previousMeasurements.add(measurement);
        if (previousMeasurements.size() > Config.getDefaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep()) {
            previousMeasurements.remove(0);
        }
        previousMeasurementsPerMac.put(measurement.mac, previousMeasurements);

        // Always apply the default strategy to keep the timestamps updated there:
        Optional<RuuviMeasurement> result = defaultStrategy.apply(measurement);

        // Apply the motion sensing strategy only if the base strategy says "no":
        if (!result.isPresent() && previousMeasurements.size() > 1) {
            final RuuviMeasurement previous = previousMeasurements.get(previousMeasurements.size() - 2);
            if (isOutsideThreshold(measurement.accelerationX, previous.accelerationX)
                || isOutsideThreshold(measurement.accelerationY, previous.accelerationY)
                || isOutsideThreshold(measurement.accelerationZ, previous.accelerationZ)) {
                result = Optional.of(measurement);
                previousOutsideOfRangePerMac.put(measurement.mac, true);
            } else if (previousOutsideOfRangePerMac.getOrDefault(measurement.mac, false)) {
                // Reset the measurements: store one more event after the values have returned to within the threshold
                result = Optional.of(measurement);
                previousOutsideOfRangePerMac.put(measurement.mac, false);
            }
        }

        return result;
    }

    private boolean isOutsideThreshold(final Double current, final Double previous) {
        if (current == null || previous == null) {
            return false;
        }
        final double upperPercentage = 1 + threshold / 100;
        final double lowerPercentage = 1 - threshold / 100;
        return current > previous * upperPercentage || current < previous * lowerPercentage;
    }


}
