package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import java.util.function.Function;

public class InfluxDBConverter {

    public static Point toInflux(RuuviMeasurement measurement) {
        return toInflux(measurement, Config.getStorageValues().equals("extended"));
    }

    public static Point toInflux(RuuviMeasurement measurement, boolean extended) {
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
        addValueIfNotNull(p, "temperature", measurement, RuuviMeasurement::getTemperature);
        addValueIfNotNull(p, "humidity", measurement, RuuviMeasurement::getHumidity);
        addValueIfNotNull(p, "pressure", measurement, RuuviMeasurement::getPressure);
        addValueIfNotNull(p, "accelerationX", measurement, RuuviMeasurement::getAccelerationX);
        addValueIfNotNull(p, "accelerationY", measurement, RuuviMeasurement::getAccelerationY);
        addValueIfNotNull(p, "accelerationZ", measurement, RuuviMeasurement::getAccelerationZ);
        addValueIfNotNull(p, "batteryVoltage", measurement, RuuviMeasurement::getBatteryVoltage);
        addValueIfNotNull(p, "txPower", measurement, RuuviMeasurement::getTxPower);
        addValueIfNotNull(p, "movementCounter", measurement, RuuviMeasurement::getMovementCounter);
        addValueIfNotNull(p, "measurementSequenceNumber", measurement, RuuviMeasurement::getMeasurementSequenceNumber);
        addValueIfNotNull(p, "rssi", measurement, RuuviMeasurement::getRssi);
        if (extended) {
            addValueIfNotNull(p, "accelerationTotal", measurement, RuuviMeasurement::getAccelerationTotal);
            addValueIfNotNull(p, "absoluteHumidity", measurement, RuuviMeasurement::getAbsoluteHumidity);
            addValueIfNotNull(p, "dewPoint", measurement, RuuviMeasurement::getDewPoint);
            addValueIfNotNull(p, "equilibriumVaporPressure", measurement, RuuviMeasurement::getEquilibriumVaporPressure);
            addValueIfNotNull(p, "airDensity", measurement, RuuviMeasurement::getAirDensity);
            addValueIfNotNull(p, "accelerationAngleFromX", measurement, RuuviMeasurement::getAccelerationAngleFromX);
            addValueIfNotNull(p, "accelerationAngleFromY", measurement, RuuviMeasurement::getAccelerationAngleFromY);
            addValueIfNotNull(p, "accelerationAngleFromZ", measurement, RuuviMeasurement::getAccelerationAngleFromZ);
        }
        return p.build();
    }

    private static void addValueIfNotNull(Point.Builder point,
                                          String name,
                                          RuuviMeasurement measurement,
                                          Function<RuuviMeasurement, ? extends Number> getter) {
        final Number value = getter.apply(measurement);
        if (value != null) {
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
