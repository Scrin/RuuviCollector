package fi.tkgwf.ruuvi.bean;

import java.util.Objects;

/**
 * This class contains all the possible fields/data acquirable from a RuuviTag
 * in a "human format", for example the temperature as a decimal number rather
 * than an integer meaning one 200th of a degree. Not all fields are necessarily
 * present depending on the data format and implementations.
 */
public class RuuviMeasurement {

    ////////////////////
    // Special values //
    ////////////////////
    /**
     * Timestamp in milliseconds, normally not populated to use local time
     */
    public Long time;

    /**
     * Friendly name for the tag
     */
    public String name;

    //////////////////////////////
    // Actual (received) values //
    //////////////////////////////
    /**
     * MAC address of the tag as seen by the receiver
     */
    public String mac;
    /**
     * Ruuvi Data format, see: https://github.com/ruuvi/ruuvi-sensor-protocols
     */
    public Integer dataFormat;
    /**
     * Temperature in Celsius
     */
    public Double temperature;
    /**
     * Relative humidity in percentage (0-100)
     */
    public Double humidity;
    /**
     * Pressure in Pa
     */
    public Double pressure;
    /**
     * Acceleration of X axis in G
     */
    public Double accelerationX;
    /**
     * Acceleration of Y axis in G
     */
    public Double accelerationY;
    /**
     * Acceleration of Z axis in G
     */
    public Double accelerationZ;
    /**
     * Battery voltage in volts
     */
    public Double batteryVoltage;
    /**
     * TX power in dBm
     */
    public Integer txPower;
    /**
     * Movement counter (incremented by interrupts from the accelerometer)
     */
    public Integer movementCounter;
    /**
     * Measurement sequence number (incremented every time a new measurement is
     * made). Useful for measurement de-duplication.
     */
    public Integer measurementSequenceNumber;
    /**
     * Tag ID as sent by the tag. In general this should be the same as the MAC.
     */
    public String tagID;
    /**
     * The RSSI at the receiver
     */
    public Integer rssi;

    /////////////////////////////////////////////////////////////////////
    // Calculated values (on the receiving side)                       //
    // All of these can be calculated based on the Actual values above //
    /////////////////////////////////////////////////////////////////////
    /**
     * Total acceleration
     */
    public Double accelerationTotal;
    /**
     * The angle between the acceleration vector and X axis
     */
    public Double accelerationAngleFromX;
    /**
     * The angle between the acceleration vector and Y axis
     */
    public Double accelerationAngleFromY;
    /**
     * The angle between the acceleration vector and Z axis
     */
    public Double accelerationAngleFromZ;
    /**
     * Absolute humidity in g/m^3
     */
    public Double absoluteHumidity;
    /**
     * Dew point in Celsius
     */
    public Double dewPoint;
    /**
     * Vapor pressure of water
     */
    public Double equilibriumVaporPressure;
    /**
     * Density of air
     */
    public Double airDensity;

    @Override
    public String toString() {
        return "RuuviMeasurement{" + "time=" + time + ", name=" + name + ", mac=" + mac + ", dataFormat=" + dataFormat + ", temperature=" + temperature + ", humidity=" + humidity + ", pressure=" + pressure + ", accelerationX=" + accelerationX + ", accelerationY=" + accelerationY + ", accelerationZ=" + accelerationZ + ", batteryVoltage=" + batteryVoltage + ", txPower=" + txPower + ", movementCounter=" + movementCounter + ", measurementSequenceNumber=" + measurementSequenceNumber + ", tagID=" + tagID + ", rssi=" + rssi + ", accelerationTotal=" + accelerationTotal + ", accelerationAngleFromX=" + accelerationAngleFromX + ", accelerationAngleFromY=" + accelerationAngleFromY + ", accelerationAngleFromZ=" + accelerationAngleFromZ + ", absoluteHumidity=" + absoluteHumidity + ", dewPoint=" + dewPoint + ", equilibriumVaporPressure=" + equilibriumVaporPressure + ", airDensity=" + airDensity + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RuuviMeasurement that = (RuuviMeasurement) o;
        return Objects.equals(time, that.time) &&
            Objects.equals(name, that.name) &&
            Objects.equals(mac, that.mac) &&
            Objects.equals(dataFormat, that.dataFormat) &&
            Objects.equals(temperature, that.temperature) &&
            Objects.equals(humidity, that.humidity) &&
            Objects.equals(pressure, that.pressure) &&
            Objects.equals(accelerationX, that.accelerationX) &&
            Objects.equals(accelerationY, that.accelerationY) &&
            Objects.equals(accelerationZ, that.accelerationZ) &&
            Objects.equals(batteryVoltage, that.batteryVoltage) &&
            Objects.equals(txPower, that.txPower) &&
            Objects.equals(movementCounter, that.movementCounter) &&
            Objects.equals(measurementSequenceNumber, that.measurementSequenceNumber) &&
            Objects.equals(tagID, that.tagID) &&
            Objects.equals(rssi, that.rssi) &&
            Objects.equals(accelerationTotal, that.accelerationTotal) &&
            Objects.equals(accelerationAngleFromX, that.accelerationAngleFromX) &&
            Objects.equals(accelerationAngleFromY, that.accelerationAngleFromY) &&
            Objects.equals(accelerationAngleFromZ, that.accelerationAngleFromZ) &&
            Objects.equals(absoluteHumidity, that.absoluteHumidity) &&
            Objects.equals(dewPoint, that.dewPoint) &&
            Objects.equals(equilibriumVaporPressure, that.equilibriumVaporPressure) &&
            Objects.equals(airDensity, that.airDensity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, name, mac, dataFormat, temperature, humidity, pressure, accelerationX, accelerationY,
            accelerationZ, batteryVoltage, txPower, movementCounter, measurementSequenceNumber, tagID, rssi,
            accelerationTotal, accelerationAngleFromX, accelerationAngleFromY, accelerationAngleFromZ, absoluteHumidity,
            dewPoint, equilibriumVaporPressure, airDensity);
    }
}
