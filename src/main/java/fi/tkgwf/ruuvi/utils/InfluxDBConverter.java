package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
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

    public static Point toInflux(RuuviMeasurement measurement) {
        return toInflux(measurement, Config.getAllowedInfluxDbFieldsPredicate(measurement.mac));
    }

    public static Point toInflux(RuuviMeasurement measurement, boolean extended) {
        if (extended) {
            return toInflux(measurement, v -> true);
        }
        return toInflux(measurement, RAW_STORAGE_VALUES::contains);
    }

    /**
     * Converts a {@link RuuviMeasurement} into an {@link org.influxdb.dto.Point}.
     *
     * @param measurement The measurement to convert
     * @param allowField A function that tells whether any given field of the given {@code RuuviMeasurement} should
     *                   be included in the resulting {@code Point} or not
     * @return A {@code Point}, ready to be saved into InfluxDB
     */
    public static Point toInflux(RuuviMeasurement measurement, Predicate<String> allowField) {
        Point.Builder p = Point.measurement(Config.getInfluxMeasurement()).tag("mac", measurement.mac);
        String name = Config.getTagName(measurement.mac);
        if (name != null) {
            p.tag("name", name);
        }
        if (measurement.dataFormat != null) {
            p.tag("dataFormat", String.valueOf(measurement.dataFormat));
        }
        if (measurement.time != null) {
            p.time(measurement.time, TimeUnit.MILLISECONDS);
        }
        addValueIfAllowed(p, "temperature", measurement, RuuviMeasurement::getTemperature, allowField);
        addValueIfAllowed(p, "humidity", measurement, RuuviMeasurement::getHumidity, allowField);
        addValueIfAllowed(p, "pressure", measurement, RuuviMeasurement::getPressure, allowField);
        addValueIfAllowed(p, "accelerationX", measurement, RuuviMeasurement::getAccelerationX, allowField);
        addValueIfAllowed(p, "accelerationY", measurement, RuuviMeasurement::getAccelerationY, allowField);
        addValueIfAllowed(p, "accelerationZ", measurement, RuuviMeasurement::getAccelerationZ, allowField);
        addValueIfAllowed(p, "batteryVoltage", measurement, RuuviMeasurement::getBatteryVoltage, allowField);
        addValueIfAllowed(p, "txPower", measurement, RuuviMeasurement::getTxPower, allowField);
        addValueIfAllowed(p, "movementCounter", measurement, RuuviMeasurement::getMovementCounter, allowField);
        addValueIfAllowed(p, "measurementSequenceNumber", measurement, RuuviMeasurement::getMeasurementSequenceNumber, allowField);
        addValueIfAllowed(p, "rssi", measurement, RuuviMeasurement::getRssi, allowField);
        addValueIfAllowed(p, "accelerationTotal", measurement, RuuviMeasurement::getAccelerationTotal, allowField);
        addValueIfAllowed(p, "absoluteHumidity", measurement, RuuviMeasurement::getAbsoluteHumidity, allowField);
        addValueIfAllowed(p, "dewPoint", measurement, RuuviMeasurement::getDewPoint, allowField);
        addValueIfAllowed(p, "equilibriumVaporPressure", measurement, RuuviMeasurement::getEquilibriumVaporPressure, allowField);
        addValueIfAllowed(p, "airDensity", measurement, RuuviMeasurement::getAirDensity, allowField);
        addValueIfAllowed(p, "accelerationAngleFromX", measurement, RuuviMeasurement::getAccelerationAngleFromX, allowField);
        addValueIfAllowed(p, "accelerationAngleFromY", measurement, RuuviMeasurement::getAccelerationAngleFromY, allowField);
        addValueIfAllowed(p, "accelerationAngleFromZ", measurement, RuuviMeasurement::getAccelerationAngleFromZ, allowField);
        return p.build();
    }

    private static void addValueIfAllowed(Point.Builder point,
                                          String name,
                                          RuuviMeasurement measurement,
                                          Function<RuuviMeasurement, ? extends Number> getter,
                                          Predicate<String> allowField) {
        final Number value = getter.apply(measurement);
        if (value != null && allowField.test(name)) {
            point.addField(name, value);
        }
    }

    public static BatchPoints toLegacyInflux(RuuviMeasurement measurement) {
        List<Point> points = new ArrayList<>();
        createAndAddLegacyFormatPointIfNotNull(points, "temperature", measurement.temperature, null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "humidity", measurement.humidity, null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "pressure", measurement.pressure, null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.accelerationX, "axis", "x");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.accelerationY, "axis", "y");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.accelerationZ, "axis", "z");
        createAndAddLegacyFormatPointIfNotNull(points, "acceleration", measurement.accelerationTotal, "axis", "total");
        createAndAddLegacyFormatPointIfNotNull(points, "batteryVoltage", measurement.batteryVoltage, null, null);
        createAndAddLegacyFormatPointIfNotNull(points, "rssi", measurement.rssi, null, null);
        return BatchPoints
            .database(Config.getInfluxDatabase())
            .tag("protocolVersion", String.valueOf(measurement.dataFormat))
            .tag("mac", measurement.mac)
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
