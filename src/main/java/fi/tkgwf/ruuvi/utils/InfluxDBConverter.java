package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

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
        addValueIfNotNull(p, "temperature", measurement.temperature);
        addValueIfNotNull(p, "humidity", measurement.humidity);
        addValueIfNotNull(p, "pressure", measurement.pressure);
        addValueIfNotNull(p, "accelerationX", measurement.accelerationX);
        addValueIfNotNull(p, "accelerationY", measurement.accelerationY);
        addValueIfNotNull(p, "accelerationZ", measurement.accelerationZ);
        addValueIfNotNull(p, "batteryVoltage", measurement.batteryVoltage);
        addValueIfNotNull(p, "txPower", measurement.txPower);
        addValueIfNotNull(p, "movementCounter", measurement.movementCounter);
        addValueIfNotNull(p, "measurementSequenceNumber", measurement.measurementSequenceNumber);
        addValueIfNotNull(p, "rssi", measurement.rssi);
        if (extended) {
            addValueIfNotNull(p, "accelerationTotal", measurement.accelerationTotal);
            addValueIfNotNull(p, "absoluteHumidity", measurement.absoluteHumidity);
            addValueIfNotNull(p, "dewPoint", measurement.dewPoint);
            addValueIfNotNull(p, "equilibriumVaporPressure", measurement.equilibriumVaporPressure);
            addValueIfNotNull(p, "airDensity", measurement.airDensity);
            addValueIfNotNull(p, "accelerationAngleFromX", measurement.accelerationAngleFromX);
            addValueIfNotNull(p, "accelerationAngleFromY", measurement.accelerationAngleFromY);
            addValueIfNotNull(p, "accelerationAngleFromZ", measurement.accelerationAngleFromZ);
        }
        return p.build();
    }

    private static void addValueIfNotNull(Point.Builder point, String name, Number value) {
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
