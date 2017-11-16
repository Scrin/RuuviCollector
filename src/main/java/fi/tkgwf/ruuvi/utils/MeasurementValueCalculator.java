package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

public class MeasurementValueCalculator {

    /**
     * Calculates and populates values that can be calculated based on other
     * values, such as total acceleration and absolute humidity
     *
     * @param measurement the measurement to populate
     * @return the supplied measurement, for convenient use in streams
     */
    public static RuuviMeasurement calculateAllValues(RuuviMeasurement measurement) {
        measurement.accelerationTotal = totalAcceleration(measurement.accelerationX, measurement.accelerationY, measurement.accelerationZ);
        measurement.absoluteHumidity = absoluteHumidity(measurement.temperature, measurement.humidity);
        measurement.dewPoint = dewPoint(measurement.temperature, measurement.humidity);
        measurement.equilibriumVaporPressure = equilibriumVaporPressure(measurement.temperature);
        measurement.airDensity = airDensity(measurement.temperature, measurement.humidity, measurement.pressure);
        return measurement;
    }

    public static Double totalAcceleration(Double accelerationX, Double accelerationY, Double accelerationZ) {
        if (accelerationX == null || accelerationY == null || accelerationZ == null) {
            return null;
        }
        return Math.sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ);
    }

    public static Double absoluteHumidity(Double temperature, Double relativeHumidity) {
        if (temperature == null || relativeHumidity == null) {
            return null;
        }
        return equilibriumVaporPressure(temperature) * relativeHumidity * 0.021674 / (273.15 + temperature);
    }

    public static Double dewPoint(Double temperature, Double relativeHumidity) {
        if (temperature == null || relativeHumidity == null || relativeHumidity == 0) {
            return null;
        }
        double v = Math.log(relativeHumidity / 100 * equilibriumVaporPressure(temperature) / 611.2);
        return -243.5 * v / (v - 17.67);
    }

    public static Double equilibriumVaporPressure(Double temperature) {
        if (temperature == null) {
            return null;
        }
        return 611.2 * Math.exp(17.67 * temperature / (243.5 + temperature));
    }

    public static Double airDensity(Double temperature, Double relativeHumidity, Double pressure) {
        if (temperature == null || relativeHumidity == null || pressure == null) {
            return null;
        }
        return 1.2929 * 273.15 / (temperature + 273.15) * (pressure - 0.3783 * relativeHumidity / 100 * equilibriumVaporPressure(temperature)) / 101300;
    }
}
