package fi.tkgwf.ruuvi.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.MDC;

import java.util.Optional;

/**
 * An exporter that writes measurements to Prometheus counters, exposed in the normal way over HTTP.
 */
public final class SLF4JExporter implements DBConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SLF4JExporter.class);

    private static final String NAMESPACE = "ruuvi";
    private static final String TAG_MAC_LABEL = NAMESPACE + ".tag_mac";
    private static final String TAG_NAME_LABEL = NAMESPACE + ".tag_name";
    private static final String DATA_FORMAT_LABEL = NAMESPACE + ".data_format";

    // normal ones
    // "The RSSI at the receiver"
    private final Marker rssi = MarkerFactory.getMarker("rssi");
    // "Temperature in Celsius"
    private final Marker temperature = MarkerFactory.getMarker("temperature");
    // "Relative humidity in percentage (0-100)"
    private final Marker humidity = MarkerFactory.getMarker("humidity");
    // "Pressure in Pa"
    private final Marker pressure = MarkerFactory.getMarker("pressure");
    // "Acceleration of X axis in G"
    private final Marker accelerationX = MarkerFactory.getMarker("acceleration_x");
    // "Acceleration of Y axis in G"
    private final Marker accelerationY = MarkerFactory.getMarker(
        "acceleration_y");
    // "Acceleration of Z axis in G"
    private final Marker accelerationZ = MarkerFactory.getMarker("acceleration_z");
    // "Battery voltage in Volts"
    private final Marker batteryVoltage = MarkerFactory.getMarker("battery_voltage");
    // "TX power in dBm"
    private final Marker txPower = MarkerFactory.getMarker("tx_power");
    // "Movement counter (incremented by interrupts from the accelerometer)"
    private final Marker movementCounter = MarkerFactory.getMarker("movement_counter");
    // "Measurement sequence number (incremented every time a new measurement is made). Useful for measurement de-duplication."
    private final Marker measurementSequenceNumber = MarkerFactory.getMarker(
        "measurement_sequence_number");

    // "enhanced" ones
    // "Total acceleration"
    private final Marker accelerationTotal = MarkerFactory.getMarker("acceleration_total");
    // "The angle between the acceleration vector and X axis"
    private final Marker accelerationAngleFromX = MarkerFactory.getMarker("acceleration_angle_from_x");
    // "The angle between the acceleration vector and Y axis"
    private final Marker accelerationAngleFromY = MarkerFactory.getMarker("acceleration_angle_from_y");
    // "The angle between the acceleration vector and Z axis"
    private final Marker accelerationAngleFromZ = MarkerFactory.getMarker("acceleration_angle_from_z");
    // "Absolute humidity in g/m^3"
    private final Marker absoluteHumidity = MarkerFactory.getMarker("absolute_humidity");
    // "Dew point in Celsius"
    private final Marker dewPoint = MarkerFactory.getMarker("dew_point");
    // "Vapor pressure of water"
    private final Marker equilibriumVaporPressure = MarkerFactory.getMarker("equilibrium_vapor_pressure");
    // "Density of air"
    private final Marker airDensity = MarkerFactory.getMarker("air_density");
    // "Time at which the last update was collected for this measurement"
    private final Marker lastUpdate = MarkerFactory.getMarker("last_update");

    public SLF4JExporter() {

    }

    @Override
    public void save(EnhancedRuuviMeasurement measurement) {

        String mac = measurement.getMac();
        String name = Optional.ofNullable(measurement.getName()).orElse(mac);
        String dataFormat = Optional.ofNullable(measurement.getDataFormat())
            .map(String::valueOf).orElse("unknown");

        logValue(rssi, measurement.getRssi(), mac, name, dataFormat);
        logValue(temperature, measurement.getTemperature(), mac, name, dataFormat);
        logValue(humidity, measurement.getHumidity(), mac, name, dataFormat);
        logValue(pressure, measurement.getPressure(), mac, name, dataFormat);
        logValue(accelerationX, measurement.getAccelerationX(), mac, name, dataFormat);
        logValue(accelerationY, measurement.getAccelerationY(), mac, name, dataFormat);
        logValue(accelerationZ, measurement.getAccelerationZ(), mac, name, dataFormat);
        logValue(batteryVoltage, measurement.getBatteryVoltage(), mac, name, dataFormat);
        logValue(txPower, measurement.getTxPower(), mac, name, dataFormat);

        logValue(measurementSequenceNumber, measurement.getMeasurementSequenceNumber(), mac, name, dataFormat);
        logValue(movementCounter, measurement.getMovementCounter(), mac, name, dataFormat);

        logValue(accelerationTotal, measurement.getAccelerationTotal(), mac, name, dataFormat);
        logValue(accelerationAngleFromX, measurement.getAccelerationAngleFromX(), mac, name, dataFormat);
        logValue(accelerationAngleFromY, measurement.getAccelerationAngleFromY(), mac, name, dataFormat);
        logValue(accelerationAngleFromZ, measurement.getAccelerationAngleFromZ(), mac, name, dataFormat);
        logValue(absoluteHumidity, measurement.getAbsoluteHumidity(), mac, name, dataFormat);
        logValue(dewPoint, measurement.getDewPoint(), mac, name, dataFormat);
        logValue(equilibriumVaporPressure, measurement.getEquilibriumVaporPressure(), mac, name, dataFormat);
        logValue(airDensity, measurement.getAirDensity(), mac, name, dataFormat);
    }

    private static void logValue(Marker marker, Number value, String mac, String name, String dataFormat) {
        if (value != null) {
            MDC.put(TAG_MAC_LABEL, mac);
            MDC.put(TAG_NAME_LABEL, name);
            MDC.put(DATA_FORMAT_LABEL, dataFormat);
            LOGGER.info(marker, String.valueOf(value.doubleValue()));
        }
    }

    @Override
    public void close() {
    }
}
