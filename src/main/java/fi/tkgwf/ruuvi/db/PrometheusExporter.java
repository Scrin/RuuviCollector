package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.Main;
import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Optional;

/**
 * An exporter that writes measurements to Prometheus counters, exposed in the normal way over HTTP.
 */
public final class PrometheusExporter implements DBConnection {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private static final String NAMESPACE = "ruuvi";
    private static final String TAG_MAC_LABEL = "tag_mac";
    private static final String TAG_NAME_LABEL = "tag_name";
    private static final String DATA_FORMAT_LABEL = "data_format";

    private final HTTPServer httpServer;

    // metadata
    private final Counter prometheusExportedCount = buildGauge("prometheus_exported",
        "The number of readings written to Prometheus collectors", Counter.build());

    // normal ones
    private final Gauge rssi = buildGauge("rssi", "The RSSI at the receiver", Gauge.build());
    private final Gauge temperature = buildGauge("temperature", "Temperature in Celsius", Gauge.build());
    private final Gauge humidity = buildGauge("humidity", "Relative humidity in percentage (0-100)", Gauge.build());
    private final Gauge pressure = buildGauge("pressure", "Pressure in Pa", Gauge.build());
    private final Gauge accelerationX = buildGauge("acceleration_x", "Acceleration of X axis in G", Gauge.build());
    private final Gauge accelerationY = buildGauge("acceleration_y", "Acceleration of Y axis in G", Gauge.build());
    private final Gauge accelerationZ = buildGauge("acceleration_z", "Acceleration of Z axis in G", Gauge.build());
    private final Gauge batteryVoltage = buildGauge("battery_voltage", "Battery voltage in Volts", Gauge.build());
    private final Gauge txPower = buildGauge("tx_power", "TX power in dBm", Gauge.build());
    private final Gauge movementCounter = buildGauge("movement_counter", "Movement counter (incremented by interrupts from the accelerometer)", Gauge.build());
    private final Gauge measurementSequenceNumber = buildGauge("measurement_sequence_number",
        "Measurement sequence number (incremented every time a new measurement is made). Useful for measurement de-duplication.", Gauge.build());

    // "enhanced" ones
    private final Gauge accelerationTotal = buildGauge("acceleration_total", "Total acceleration", Gauge.build());
    private final Gauge accelerationAngleFromX = buildGauge("acceleration_angle_from_x", "The angle between the acceleration vector and X axis", Gauge.build());
    private final Gauge accelerationAngleFromY = buildGauge("acceleration_angle_from_y", "The angle between the acceleration vector and Y axis", Gauge.build());
    private final Gauge accelerationAngleFromZ = buildGauge("acceleration_angle_from_z", "The angle between the acceleration vector and Z axis", Gauge.build());
    private final Gauge absoluteHumidity = buildGauge("absolute_humidity", "Absolute humidity in g/m^3", Gauge.build());
    private final Gauge dewPoint = buildGauge("dew_point", "Dew point in Celsius", Gauge.build());
    private final Gauge equilibriumVaporPressure = buildGauge("equilibrium_vapor_pressure", "Vapor pressure of water", Gauge.build());
    private final Gauge airDensity = buildGauge("air_density", "Density of air", Gauge.build());

    private final Gauge lastUpdate = buildGauge("last_update",
        "Time at which the last update was collected for this measurement", Gauge.build());

    public PrometheusExporter(int port) {
        LOG.debug("Initialising PrometheusExporter, serving metrics on port " + port);
        try {
            httpServer = new HTTPServer(port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Prometheus exporter HTTP server", e);
        }
    }

    @SuppressWarnings("rawtypes") // SimpleCollector must be raw to satisfy the Builder<B, C> signature
    private static <C extends SimpleCollector, B extends SimpleCollector.Builder<B, C>> C buildGauge(String name, String helpText, B builder) {
        return builder
            .namespace(NAMESPACE)
            .name(name)
            .help(helpText)
            .labelNames(TAG_MAC_LABEL, TAG_NAME_LABEL, DATA_FORMAT_LABEL)
            .create()
            .register();
    }

    @Override
    public void save(EnhancedRuuviMeasurement measurement) {

        String mac = measurement.getMac();
        String name = Optional.ofNullable(measurement.getName()).orElse(mac);
        String dataFormat = Optional.ofNullable(measurement.getDataFormat())
            .map(String::valueOf).orElse("unknown");

        prometheusExportedCount.labels(mac, name, dataFormat).inc();

        setValue(rssi.labels(mac, name, dataFormat), measurement.getRssi());
        setValue(temperature.labels(mac, name, dataFormat), measurement.getTemperature());
        setValue(humidity.labels(mac, name, dataFormat), measurement.getHumidity());
        setValue(pressure.labels(mac, name, dataFormat), measurement.getPressure());
        setValue(accelerationX.labels(mac, name, dataFormat), measurement.getAccelerationX());
        setValue(accelerationY.labels(mac, name, dataFormat), measurement.getAccelerationY());
        setValue(accelerationZ.labels(mac, name, dataFormat), measurement.getAccelerationZ());
        setValue(batteryVoltage.labels(mac, name, dataFormat), measurement.getBatteryVoltage());
        setValue(txPower.labels(mac, name, dataFormat), measurement.getTxPower());

        setValue(measurementSequenceNumber.labels(mac, name, dataFormat), measurement.getMeasurementSequenceNumber());
        setValue(movementCounter.labels(mac, name, dataFormat), measurement.getMovementCounter());

        setValue(accelerationTotal.labels(mac, name, dataFormat), measurement.getAccelerationTotal());
        setValue(accelerationAngleFromX.labels(mac, name, dataFormat), measurement.getAccelerationAngleFromX());
        setValue(accelerationAngleFromY.labels(mac, name, dataFormat), measurement.getAccelerationAngleFromY());
        setValue(accelerationAngleFromZ.labels(mac, name, dataFormat), measurement.getAccelerationAngleFromZ());
        setValue(absoluteHumidity.labels(mac, name, dataFormat), measurement.getAbsoluteHumidity());
        setValue(dewPoint.labels(mac, name, dataFormat), measurement.getDewPoint());
        setValue(equilibriumVaporPressure.labels(mac, name, dataFormat), measurement.getEquilibriumVaporPressure());
        setValue(airDensity.labels(mac, name, dataFormat), measurement.getAirDensity());

        lastUpdate.labels(mac, name, dataFormat).setToCurrentTime();
    }

    private static void setValue(Gauge.Child gauge, Number value) {
        if (value != null) {
            gauge.set(value.doubleValue());
        }
    }

    @Override
    public void close() {
        httpServer.stop();
    }
}
