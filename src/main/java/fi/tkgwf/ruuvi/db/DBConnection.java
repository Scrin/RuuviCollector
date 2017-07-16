package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.InfluxDBData;

public interface DBConnection {

    boolean post(InfluxDBData measurement);
}
