package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;

public class MeasurementValueCalculator {

    /**
     * Calculates values that can be calculated based on other values, such as
     * total acceleration and absolute humidity
     *
     * @param measurement the measurement
     * @return The supplied Measurement
     */
    public static EnhancedRuuviMeasurement calculateAllValues(EnhancedRuuviMeasurement measurement) {
        measurement.setAbsoluteHumidity(absoluteHumidity(measurement.getTemperature(), measurement.getHumidity()));
        measurement.setDewPoint(dewPoint(measurement.getTemperature(), measurement.getHumidity()));
        measurement.setEquilibriumVaporPressure(equilibriumVaporPressure(measurement.getTemperature()));
        measurement.setAirDensity(airDensity(measurement.getTemperature(), measurement.getHumidity(), measurement.getPressure()));
        measurement.setAccelerationTotal(totalAcceleration(measurement.getAccelerationX(), measurement.getAccelerationY(), measurement.getAccelerationZ()));
        measurement.setAccelerationAngleFromX(angleBetweenVectorComponentAndAxis(measurement.getAccelerationX(), measurement.getAccelerationTotal()));
        measurement.setAccelerationAngleFromY(angleBetweenVectorComponentAndAxis(measurement.getAccelerationY(), measurement.getAccelerationTotal()));
        measurement.setAccelerationAngleFromZ(angleBetweenVectorComponentAndAxis(measurement.getAccelerationZ(), measurement.getAccelerationTotal()));
        return measurement;
    }

    /**
     * Calculates the total acceleration strength
     *
     * @param accelerationX
     * @param accelerationY
     * @param accelerationZ
     * @return The total acceleration strength
     */
    public static Double totalAcceleration(Double accelerationX, Double accelerationY, Double accelerationZ) {
        if (accelerationX == null || accelerationY == null || accelerationZ == null) {
            return null;
        }
        return Math.sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ);
    }

    /**
     * Calculates the angle between a vector component and the corresponding
     * axis
     *
     * @param vectorComponent Vector component
     * @param vectorLength Vector length
     * @return Angle between the components axis and the vector, in degrees
     */
    public static Double angleBetweenVectorComponentAndAxis(Double vectorComponent, Double vectorLength) {
        if (vectorComponent == null || vectorLength == null || vectorLength == 0) {
            return null;
        }
        return Math.toDegrees(Math.acos(vectorComponent / vectorLength));
    }

    /**
     * Calculates the absolute humidity
     *
     * @param temperature Temperature in Celsius
     * @param relativeHumidity Relative humidity % (range 0-100)
     * @return The absolute humidity in g/m^3
     */
    public static Double absoluteHumidity(Double temperature, Double relativeHumidity) {
        if (temperature == null || relativeHumidity == null) {
            return null;
        }
        return equilibriumVaporPressure(temperature) * relativeHumidity * 0.021674 / (273.15 + temperature);
    }

    /**
     * Calculates the dew point
     *
     * @param temperature Temperature in Celsius
     * @param relativeHumidity Relative humidity % (range 0-100)
     * @return The dew point in Celsius
     */
    public static Double dewPoint(Double temperature, Double relativeHumidity) {
        if (temperature == null || relativeHumidity == null || relativeHumidity == 0) {
            return null;
        }
        double v = Math.log(relativeHumidity / 100 * equilibriumVaporPressure(temperature) / 611.2);
        return -243.5 * v / (v - 17.67);
    }

    /**
     * Calculates the equilibrium vapor pressure of water
     *
     * @param temperature Temperature in Celsius
     * @return The vapor pressure in Pa
     */
    public static Double equilibriumVaporPressure(Double temperature) {
        if (temperature == null) {
            return null;
        }
        return 611.2 * Math.exp(17.67 * temperature / (243.5 + temperature));
    }

    /**
     * Calculates the air density
     *
     * @param temperature Temperature in Celsius
     * @param relativeHumidity Relative humidity % (range 0-100)
     * @param pressure Pressure in pa
     * @return The air density in kg/m^3
     */
    public static Double airDensity(Double temperature, Double relativeHumidity, Double pressure) {
        if (temperature == null || relativeHumidity == null || pressure == null) {
            return null;
        }
        return 1.2929 * 273.15 / (temperature + 273.15) * (pressure - 0.3783 * relativeHumidity / 100 * equilibriumVaporPressure(temperature)) / 101300;
    }
}
