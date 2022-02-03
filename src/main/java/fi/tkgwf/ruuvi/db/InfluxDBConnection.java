package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.utils.InfluxDBConverter;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

public class InfluxDBConnection implements DBConnection {

    private final InfluxDB influxDB;

    public InfluxDBConnection() {
        this(
                Config.getInfluxUrl(),
                Config.getInfluxUser(),
                Config.getInfluxPassword(),
                Config.getInfluxDatabase(),
                Config.getInfluxRetentionPolicy(),
                Config.isInfluxGzip(),
                Config.isInfluxBatch(),
                Config.getInfluxBatchMaxSize(),
                Config.getInfluxBatchMaxTimeMs()
        );
    }

    public InfluxDBConnection(
            String url,
            String user,
            String password,
            String database,
            String retentionPolicy,
            boolean gzip,
            boolean batch,
            int batchSize,
            int batchTime
    ) {
        influxDB = InfluxDBFactory.connect(url, user, password).setDatabase(database).setRetentionPolicy(retentionPolicy);
        if (gzip) {
            influxDB.enableGzip();
        } else {
            influxDB.disableGzip();
        }
        if (batch) {
            influxDB.enableBatch(batchSize, batchTime, TimeUnit.MILLISECONDS);
        } else {
            influxDB.disableBatch();
        }
    }

    @Override
    public void save(EnhancedRuuviMeasurement measurement) {
        Point point = InfluxDBConverter.toInflux(measurement);
        influxDB.write(point);
    }

    @Override
    public void close() {
        influxDB.close();
    }
}
