package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.InfluxDBData;
import org.apache.log4j.Logger;

public class DummyDBConnection implements DBConnection {
    
    private static final Logger LOG = Logger.getLogger(DummyDBConnection.class);

    @Override
    public boolean post(InfluxDBData measurement) {
        LOG.debug(measurement);
        return true;
    }

}
