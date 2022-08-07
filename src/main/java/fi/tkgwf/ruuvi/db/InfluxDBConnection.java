package fi.tkgwf.ruuvi.db;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.utils.InfluxDBConverter;

public class InfluxDBConnection implements DBConnection {

    private final InfluxDBClient influxDB;

    public InfluxDBConnection() {
        this(
                Config.getInfluxUrl(),
                Config.getInfluxApiToken(),
                "ruuvi",
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
            String apiToken,
            String org,
            String bucket,
            String retentionPolicy,
            boolean gzip,
            boolean batch,
            int batchSize,
            int batchTime
    ) {
        influxDB = InfluxDBClientFactory.create(url, apiToken.toCharArray(), org, bucket);
    }

    @Override
    public void save(EnhancedRuuviMeasurement measurement) {
        Point point = InfluxDBConverter.toInflux(measurement);
        WriteApiBlocking writeApi = influxDB.getWriteApiBlocking();
        writeApi.writePoint(point);
    }

    @Override
    public void close() {
        influxDB.close();
    }
}
