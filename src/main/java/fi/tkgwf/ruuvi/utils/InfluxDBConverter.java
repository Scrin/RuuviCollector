package fi.tkgwf.ruuvi.utils;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.util.function.Function;
import java.util.function.Predicate;

public class InfluxDBConverter {
    public static final Collection<String> RAW_STORAGE_VALUES;

    static {
        final Collection<String> rawStorageValues = new HashSet<>();
        rawStorageValues.add("temperature");
        rawStorageValues.add("humidity");
        rawStorageValues.add("pressure");
        rawStorageValues.add("accelerationX");
        rawStorageValues.add("accelerationY");
        rawStorageValues.add("accelerationZ");
        rawStorageValues.add("batteryVoltage");
        rawStorageValues.add("txPower");
        rawStorageValues.add("movementCounter");
        rawStorageValues.add("measurementSequenceNumber");
        rawStorageValues.add("rssi");
        RAW_STORAGE_VALUES = Collections.unmodifiableCollection(rawStorageValues);
    }

    public static Point toInflux(EnhancedRuuviMeasurement measurement) {
        return toInflux(measurement, Config.getAllowedInfluxDbFieldsPredicate(measurement.getMac()));
    }

    public static Point toInflux(EnhancedRuuviMeasurement measurement, boolean extended) {
        if (extended) {
            return toInflux(measurement, v -> true);
        }
        return toInflux(measurement, RAW_STORAGE_VALUES::contains);
    }

    /**
     * Converts a {@link EnhancedRuuviMeasurement} into an {@link com.influxdb.client.write.Point}.
     *
     * @param measurement The measurement to convert
     * @param allowField A function that tells whether any given field of the given {@code RuuviMeasurement} should
     *                   be included in the resulting {@code Point} or not
     * @return A {@code Point}, ready to be saved into InfluxDB
     */
    public static Point toInflux(EnhancedRuuviMeasurement measurement, Predicate<String> allowField) {
        Point p = Point.measurement(Config.getInfluxMeasurement()).addTag("mac", measurement.getMac());
        if (measurement.getName() != null) {
            p.addTag("name", measurement.getName());
        }
        if (measurement.getDataFormat() != null) {
            p.addTag("dataFormat", String.valueOf(measurement.getDataFormat()));
        }
        if (StringUtils.isNotBlank(measurement.getReceiver())) {
            p.addTag("receiver", measurement.getReceiver());
        }
        if (measurement.getTime() != null) {
            p.time(measurement.getTime(), WritePrecision.MS);
        }
        addValueIfAllowed(p, "temperature", measurement, EnhancedRuuviMeasurement::getTemperature, allowField);
        addValueIfAllowed(p, "humidity", measurement, EnhancedRuuviMeasurement::getHumidity, allowField);
        addValueIfAllowed(p, "pressure", measurement, EnhancedRuuviMeasurement::getPressure, allowField);
        addValueIfAllowed(p, "accelerationX", measurement, EnhancedRuuviMeasurement::getAccelerationX, allowField);
        addValueIfAllowed(p, "accelerationY", measurement, EnhancedRuuviMeasurement::getAccelerationY, allowField);
        addValueIfAllowed(p, "accelerationZ", measurement, EnhancedRuuviMeasurement::getAccelerationZ, allowField);
        addValueIfAllowed(p, "batteryVoltage", measurement, EnhancedRuuviMeasurement::getBatteryVoltage, allowField);
        addValueIfAllowed(p, "txPower", measurement, EnhancedRuuviMeasurement::getTxPower, allowField);
        addValueIfAllowed(p, "movementCounter", measurement, EnhancedRuuviMeasurement::getMovementCounter, allowField);
        addValueIfAllowed(p, "measurementSequenceNumber", measurement, EnhancedRuuviMeasurement::getMeasurementSequenceNumber, allowField);
        addValueIfAllowed(p, "rssi", measurement, EnhancedRuuviMeasurement::getRssi, allowField);
        addValueIfAllowed(p, "accelerationTotal", measurement, EnhancedRuuviMeasurement::getAccelerationTotal, allowField);
        addValueIfAllowed(p, "absoluteHumidity", measurement, EnhancedRuuviMeasurement::getAbsoluteHumidity, allowField);
        addValueIfAllowed(p, "dewPoint", measurement, EnhancedRuuviMeasurement::getDewPoint, allowField);
        addValueIfAllowed(p, "equilibriumVaporPressure", measurement, EnhancedRuuviMeasurement::getEquilibriumVaporPressure, allowField);
        addValueIfAllowed(p, "airDensity", measurement, EnhancedRuuviMeasurement::getAirDensity, allowField);
        addValueIfAllowed(p, "accelerationAngleFromX", measurement, EnhancedRuuviMeasurement::getAccelerationAngleFromX, allowField);
        addValueIfAllowed(p, "accelerationAngleFromY", measurement, EnhancedRuuviMeasurement::getAccelerationAngleFromY, allowField);
        addValueIfAllowed(p, "accelerationAngleFromZ", measurement, EnhancedRuuviMeasurement::getAccelerationAngleFromZ, allowField);
        return p;
    }

    private static void addValueIfAllowed(Point point,
                                          String name,
                                          EnhancedRuuviMeasurement measurement,
                                          Function<EnhancedRuuviMeasurement, ? extends Number> getter,
                                          Predicate<String> allowField) {
        final Number value = getter.apply(measurement);
        if (value != null && allowField.test(name)) {
            point.addField(name, value);
        }
    }

    private static void createAndAddLegacyFormatPointIfNotNull(List<Point> points, String measurement, Number value, String extraTagKey, String extraTagValue) {
        if (value != null) {
            Point p = Point.measurement(measurement).addField("value", value);
            if (extraTagValue != null) {
                p.addTag(extraTagKey, extraTagValue);
            }
            points.add(p);
        }
    }
}
