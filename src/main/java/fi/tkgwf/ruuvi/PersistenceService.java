package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;

class PersistenceService implements AutoCloseable {
    private final DBConnection db;
    private final LimitingStrategy limitingStrategy;

    PersistenceService() {
        this(Config.getDBConnection(), Config.getLimitingStrategy());
    }

    PersistenceService(final DBConnection db, final LimitingStrategy strategy) {
        this.db = db;
        this.limitingStrategy = strategy;
    }

    @Override
    public void close() {
        db.close();
    }

    void store(final RuuviMeasurement measurement) {
        this.limitingStrategy.apply(measurement).ifPresent(db::save);
    }

}
