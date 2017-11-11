package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.utils.InfluxDBConverter;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;

public class InfluxDBConnection implements DBConnection {

    private final InfluxDB influxDB;

    public InfluxDBConnection() {
        influxDB = InfluxDBFactory.connect(Config.getInfluxUrl(), Config.getInfluxUser(), Config.getInfluxPassword());
        influxDB.setDatabase(Config.getInfluxDatabase());
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS); // TODO: make these configurable
    }

    @Override
    public void save(RuuviMeasurement measurement) {
        BatchPoints points = InfluxDBConverter.toLegacyInflux(measurement);
        influxDB.write(points);
    }
}
