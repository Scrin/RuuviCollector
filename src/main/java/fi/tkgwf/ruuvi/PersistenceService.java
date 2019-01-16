package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DiscardUntilEnoughTimeHasElapsedStrategy;

class PersistenceService implements AutoCloseable {
    private final DBConnection db;
    private final LimitingStrategy limitingStrategy;

    PersistenceService() {
        this(Config.getDBConnection());
    }

    PersistenceService(final DBConnection db) {
        this.db = db;
        this.limitingStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();
    }

    @Override
    public void close() {
        db.close();
    }

    void store(final RuuviMeasurement measurement) {
        this.limitingStrategy.apply(measurement).ifPresent(db::save);
    }

}
