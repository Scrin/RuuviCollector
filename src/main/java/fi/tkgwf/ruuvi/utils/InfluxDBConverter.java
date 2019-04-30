package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
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
     * Converts a {@link EnhancedRuuviMeasurement} into an {@link org.influxdb.dto.Point}.
     *
     * @param measurement The measurement to convert
     * @param allowField A function that tells whether any given field of the given {@code RuuviMeasurement} should
     *                   be included in the resulting {@code Point} or not
     * @return A {@code Point}, ready to be saved into InfluxDB
     */
    public static Point toInflux(EnhancedRuuviMeasurement measurement, Predicate<String> allowField) {
        Point.Builder p = Point.measurement(Config.getInfluxMeasurement()).tag("mac", measurement.getMac());
        if (measurement.getName() != null) {
            p.tag("name", measurement.getName());
        }
        if (measurement.getDataFormat() != null) {
            p.tag("dataFormat", String.valueOf(measurement.getDataFormat()));
        }
        if (measurement.getTime() != null) {
            p.time(measurement.getTime(), TimeUnit.MILLISECONDS);
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
        return p.build();
    }

    private static void addValueIfAllowed(Point.Builder point,
                                          String name,
                                          EnhancedRuuviMeasurement measurement,
                                          Function<EnhancedRuuviMeasurement, ? extends Number> getter,
                                          Predicate<String> allowField) {
        final Number value = getter.apply(measurement);
        if (value != null && allowField.test(name)) {
            point.addField(name, value);
        }
    }

    public static BatchPoints toLegacyInflux(EnhancedRuuviMeasurement measurement) {
        List<Point> points = new ArrayList<>();
        createAndAddLegacyFormatPointIfNotNull(points, "temperature", measurement.getTemperature(), null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "humidity", measurement.getHumidity(), null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "pressure", measurement.getPressure(), null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.getAccelerationX(), "axis", "x");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.getAccelerationY(), "axis", "y");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.getAccelerationZ(), "axis", "z");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.getAccelerationTotal(), "axis", "total");
        createAndAddLegacyFormatPointIfNotNull(points, "batteryVoltage", measurement.getBatteryVoltage(), null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "rssi", measurement.getRssi(), null, null);
        return BatchPoints
            .database(Config.getInfluxDatabase())
            .tag("protocolVersion", String.valueOf(measurement.getDataFormat()))
            .tag("mac", measurement.getMac())
            .points(points.toArray(new Point[points.size()]))
            .build();
    }

    private static void createAndAddLegacyFormatPointIfNotNull(List<Point> points, String measurement, Number value, String extraTagKey, String extraTagValue) {
        if (value != null) {
            Point.Builder p = Point.measurement(measurement).addField("value", value);
            if (extraTagValue != null) {
                p.tag(extraTagKey, extraTagValue);
            }
            points.add(p.build());
        }
    }
}
