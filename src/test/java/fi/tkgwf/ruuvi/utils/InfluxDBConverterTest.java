package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfluxDBConverterTest {

    @Test
    void toInfluxShouldGiveExtendedValues() {
        final RuuviMeasurement measurement = createMeasurement();
        final Point point = InfluxDBConverter.toInflux(measurement);
        assertFalse(point.toString().contains("null"));
    }

    @Test
    void toInfluxTrueShouldGiveExtendedValues() {
        final RuuviMeasurement measurement = createMeasurement();
        final Point point = InfluxDBConverter.toInflux(measurement, true);
        assertFalse(point.toString().contains("null"));
    }

    @Test
    void toInfluxFalseShouldGiveOnlyRawValues() {
        final RuuviMeasurement measurement = createMeasurement();
        final Point point = InfluxDBConverter.toInflux(measurement, false);
        assertFalse(point.toString().contains("accelerationTotal"));
        assertFalse(point.toString().contains("absoluteHumidity"));
        assertFalse(point.toString().contains("dewPoint"));
        assertFalse(point.toString().contains("equilibriumVaporPressure"));
        assertFalse(point.toString().contains("airDensity"));
        assertFalse(point.toString().contains("accelerationAngleFromX"));
        assertFalse(point.toString().contains("accelerationAngleFromY"));
        assertFalse(point.toString().contains("accelerationAngleFromZ"));
        assertTrue(point.toString().contains("rssi=13"));
    }

    @Test
    void toInfluxWithAllowFunctionShouldIncludeRequiredValuesOnly() {
        final RuuviMeasurement measurement = createMeasurement();
        final Function<String, Boolean> allowFunction = fieldName ->
            fieldName.equals("accelerationTotal")
                || fieldName.equals("measurementSequenceNumber")
                || fieldName.equals("txPower");
        measurement.measurementSequenceNumber = null;
        final Point point = InfluxDBConverter.toInflux(measurement, allowFunction);
        assertTrue(point.toString().contains("mac")); // cannot be disabled
        assertTrue(point.toString().contains("dataFormat")); // cannot be disabled
        assertTrue(point.toString().contains("time")); // cannot be disabled
        assertFalse(point.toString().contains("temperature"));
        assertFalse(point.toString().contains("humidity"));
        assertFalse(point.toString().contains("pressure"));
        assertFalse(point.toString().contains("accelerationX"));
        assertFalse(point.toString().contains("accelerationY"));
        assertFalse(point.toString().contains("accelerationZ"));
        assertFalse(point.toString().contains("batteryVoltage"));
        assertTrue(point.toString().contains("txPower"));
        assertFalse(point.toString().contains("movementCounter"));
        assertFalse(point.toString().contains("measurementSequenceNumber")); // allowed but null -> should not be included
        assertFalse(point.toString().contains("rssi"));
        assertTrue(point.toString().contains("accelerationTotal"));
        assertFalse(point.toString().contains("absoluteHumidity"));
        assertFalse(point.toString().contains("dewPoint"));
        assertFalse(point.toString().contains("equilibriumVaporPressure"));
        assertFalse(point.toString().contains("airDensity"));
        assertFalse(point.toString().contains("accelerationAngleFromX"));
        assertFalse(point.toString().contains("accelerationAngleFromY"));
        assertFalse(point.toString().contains("accelerationAngleFromZ"));
    }

    private static RuuviMeasurement createMeasurement() {
        final RuuviMeasurement measurement = new RuuviMeasurement();
        double d = 1d;
        measurement.mac = "AAAAAAAAAAAA";
        measurement.dataFormat = (int) d++;
        measurement.time = (long) d++;
        measurement.temperature = d++;
        measurement.humidity = d++;
        measurement.pressure = d++;
        measurement.accelerationX = d++;
        measurement.accelerationY = d++;
        measurement.accelerationZ = d++;
        measurement.batteryVoltage = d++;
        measurement.txPower = (int) d++;
        measurement.movementCounter = (int) d++;
        measurement.measurementSequenceNumber = (int) d++;
        measurement.rssi = (int) d++;
        measurement.accelerationTotal = d++;
        measurement.absoluteHumidity = d++;
        measurement.dewPoint = d++;
        measurement.equilibriumVaporPressure = d++;
        measurement.airDensity = d++;
        measurement.accelerationAngleFromX = d++;
        measurement.accelerationAngleFromY = d++;
        measurement.accelerationAngleFromZ = d;
        return measurement;
    }
}
