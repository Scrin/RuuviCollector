package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;

public interface DBConnection {

    /**
     * Saves the measurement
     *
     * @param measurement
     */
    void save(EnhancedRuuviMeasurement measurement);

    /**
     * Closes the DB connection
     */
    void close();
}
