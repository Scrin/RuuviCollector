package fi.tkgwf.ruuvi.bean;

/**
 * This class contains all the possible fields/data acquirable from a RuuviTag
 * in a "human format", for example the temperature as a decimal number rather
 * than an integer meaning one 200th of a degree. Not all fields are necessarily
 * present depending on the data format and implementations.
 */
public class RuuviMeasurement {

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
    public Double relativeHumidity;
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

    ///////////////////////////////////////////////
    // Calculated values (on the receiving side) //
    ///////////////////////////////////////////////
    /**
     * Total acceleration (Calculated value)
     */
    public Double accelerationTotal;
    /**
     * Absolute humidity in g/m^3
     */
    public Double absoluteHumidity;
    /**
     * Dew point in Celsius
     */
    public Double dewPoint;
}
