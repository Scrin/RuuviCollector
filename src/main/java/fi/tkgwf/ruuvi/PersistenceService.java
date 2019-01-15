package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;

import java.util.HashMap;
import java.util.Map;

class PersistenceService implements AutoCloseable {
    /**
     * Contains the MAC address as key, and the timestamp of last sent update as value
     */
    private final Map<String, Long> updatedMacs = new HashMap<>();
    private final long updateLimit = Config.getMeasurementUpdateLimit();
    private final DBConnection db;

    PersistenceService() {
        this(Config.getDBConnection());
    }

    PersistenceService(final DBConnection db) {
        this.db = db;
    }

    @Override
    public void close() {
        db.close();
    }

    void store(final RuuviMeasurement measurement) {
        if (shouldUpdate(measurement.mac)) {
            db.save(measurement);
        }
    }

    private boolean shouldUpdate(final String mac) {
        final Long lastUpdate = updatedMacs.get(mac);
        final long currentTime = Config.currentTimeMillis();
        if (lastUpdate == null || lastUpdate + updateLimit < currentTime) {
            updatedMacs.put(mac, currentTime);
            return true;
        }
        return false;
    }
}
