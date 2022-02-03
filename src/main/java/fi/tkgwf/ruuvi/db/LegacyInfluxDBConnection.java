package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.utils.InfluxDBConverter;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;

public class LegacyInfluxDBConnection implements DBConnection {

    private final InfluxDB influxDB;

    public LegacyInfluxDBConnection() {
        influxDB = InfluxDBFactory.connect(Config.getInfluxUrl(), Config.getInfluxUser(), Config.getInfluxPassword());
        influxDB.setDatabase(Config.getInfluxDatabase());
        influxDB.enableGzip();
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS); // TODO: make these configurable
    }

    @Override
    public void save(EnhancedRuuviMeasurement measurement) {
        BatchPoints points = InfluxDBConverter.toLegacyInflux(measurement);
        influxDB.write(points);
    }

    @Override
    public void close() {
        influxDB.close();
    }
}
