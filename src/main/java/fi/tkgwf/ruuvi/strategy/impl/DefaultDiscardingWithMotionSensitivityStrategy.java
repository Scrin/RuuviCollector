package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
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
 * The acceleration bounds are defined as values compared to the previous measurement
 * with {@link Config#getDefaultWithMotionSensitivityStrategyThreshold()}.
 * </p><p>
 * The limit is applied separately to all the different devices sending data, i.e. per MAC address.
 * </p>
 */
public class DefaultDiscardingWithMotionSensitivityStrategy implements LimitingStrategy {
    private final DiscardUntilEnoughTimeHasElapsedStrategy defaultStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();

    private final Double threshold = Config.getDefaultWithMotionSensitivityStrategyThreshold();
    private final Map<String, List<EnhancedRuuviMeasurement>> previousMeasurementsPerMac = new HashMap<>();
    private final Map<String, Boolean> previousOutsideOfRangePerMac = new HashMap<>();

    @Override
    public Optional<EnhancedRuuviMeasurement> apply(final EnhancedRuuviMeasurement measurement) {
        final List<EnhancedRuuviMeasurement> previousMeasurements = previousMeasurementsPerMac.getOrDefault(measurement.getMac(), new LinkedList<>());
        previousMeasurements.add(measurement);
        if (previousMeasurements.size() > Config.getDefaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep()) {
            previousMeasurements.remove(0);
        }
        previousMeasurementsPerMac.put(measurement.getMac(), previousMeasurements);

        // Always apply the default strategy to keep the timestamps updated there:
        Optional<EnhancedRuuviMeasurement> result = defaultStrategy.apply(measurement);

        // Apply the motion sensing strategy only if the base strategy says "no":
        if (!result.isPresent() && previousMeasurements.size() > 1) {
            final EnhancedRuuviMeasurement previous = previousMeasurements.get(previousMeasurements.size() - 2);
            if (isOutsideThreshold(measurement.getAccelerationX(), previous.getAccelerationX())
                || isOutsideThreshold(measurement.getAccelerationY(), previous.getAccelerationY())
                || isOutsideThreshold(measurement.getAccelerationZ(), previous.getAccelerationZ())) {
                result = Optional.of(measurement);
                previousOutsideOfRangePerMac.put(measurement.getMac(), true);
            } else if (previousOutsideOfRangePerMac.getOrDefault(measurement.getMac(), false)) {
                // Reset the measurements: store one more event after the values have returned to within the threshold
                result = Optional.of(measurement);
                previousOutsideOfRangePerMac.put(measurement.getMac(), false);
            }
        }

        return result;
    }

    private boolean isOutsideThreshold(final Double current, final Double previous) {
        if (current == null || previous == null) {
            return false;
        }
        final double upperBound = previous + threshold;
        final double lowerBound = previous - threshold;
        return current > upperBound || current < lowerBound;
    }


}
