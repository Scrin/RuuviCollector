package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

public interface DBConnection {

    /**
     * Saves the measurement
     *
     * @param measurement
     * @return true on success, false on failure
     */
    boolean save(RuuviMeasurement measurement);
}
