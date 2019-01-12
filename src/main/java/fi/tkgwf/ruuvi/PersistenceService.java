package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;

class PersistenceService implements AutoCloseable {
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
        db.save(measurement);
    }
}
