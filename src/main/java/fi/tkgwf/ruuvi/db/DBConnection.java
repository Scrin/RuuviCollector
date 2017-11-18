package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

public interface DBConnection {

    /**
     * Saves the measurement
     *
     * @param measurement
     */
    void save(RuuviMeasurement measurement);

    /**
     * Closes the DB connection
     */
    void close();
}
