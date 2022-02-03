package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The default limiting strategy: discard measurements that are coming in too fast.
 * The time limit is defined as {@link Config#getMeasurementUpdateLimit()}.
 * The limit is applied separately to all the different devices sending data, i.e. per MAC address.
 */
public class DiscardUntilEnoughTimeHasElapsedStrategy implements LimitingStrategy {
    /**
     * Contains the MAC address as key, and the timestamp of last sent update as value
     */
    private final Map<String, Long> updatedMacs = new HashMap<>();
    private final long updateLimit = Config.getMeasurementUpdateLimit();

    @Override
    public Optional<EnhancedRuuviMeasurement> apply(final EnhancedRuuviMeasurement measurement) {
        if (!shouldUpdate(measurement.getMac())) {
            return Optional.empty();
        }
        return Optional.of(measurement);
    }

    private boolean shouldUpdate(final String mac) {
        final Long lastUpdate = updatedMacs.get(mac);
        final long currentTime = Config.getTimestampProvider().get();
        if (lastUpdate == null || lastUpdate + updateLimit < currentTime) {
            updatedMacs.put(mac, currentTime);
            return true;
        }
        return false;
    }
}
