package fi.tkgwf.ruuvi.strategy;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;

import java.util.Optional;
import java.util.function.Function;

public interface LimitingStrategy extends Function<EnhancedRuuviMeasurement, Optional<EnhancedRuuviMeasurement>> {

    /**
     * Applies a limiting strategy to the given measurement.
     *
     * @param measurement A measurement considered for persisting.
     * @return An {@link Optional#empty()} if the strategy is to not store this measurement,
     * or a non-empty {@link Optional} item to be stored.
     *
     * Note that depending on the strategy the returned {@link EnhancedRuuviMeasurement} may or may not be
     * the same object that was given in as a parameter: it might be the strategy to, say, calculate
     * some averages or maximum values of some of the previous measurements instead.
     */
    @Override
    Optional<EnhancedRuuviMeasurement> apply(EnhancedRuuviMeasurement measurement);
}
