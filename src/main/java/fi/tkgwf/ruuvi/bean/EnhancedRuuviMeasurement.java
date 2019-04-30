package fi.tkgwf.ruuvi.bean;

import fi.tkgwf.ruuvi.common.bean.RuuviMeasurement;

/**
 * This class contains all the possible fields/data acquirable from a RuuviTag
 * in a "human format", for example the temperature as a decimal number rather
 * than an integer meaning one 200th of a degree. Not all fields are necessarily
 * present depending on the data format and implementations.
 */
public class EnhancedRuuviMeasurement extends RuuviMeasurement {
    
    public EnhancedRuuviMeasurement() {
        super();
    }

    public EnhancedRuuviMeasurement(RuuviMeasurement m) {
        this();
        this.setDataFormat(m.getDataFormat());
        this.setTemperature(m.getTemperature());
        this.setHumidity(m.getHumidity());
        this.setPressure(m.getPressure());
        this.setAccelerationX(m.getAccelerationX());
        this.setAccelerationY(m.getAccelerationY());
        this.setAccelerationZ(m.getAccelerationZ());
        this.setBatteryVoltage(m.getBatteryVoltage());
        this.setTxPower(m.getTxPower());
        this.setMovementCounter(m.getMovementCounter());
        this.setMeasurementSequenceNumber(m.getMeasurementSequenceNumber());
    }

    /**
     * Timestamp in milliseconds, normally not populated to use local time
     */
    private Long time;
    /**
     * Friendly name for the tag
     */
    private String name;
    /**
     * MAC address of the tag as seen by the receiver
     */
    private String mac;
    /**
     * The RSSI at the receiver
     */
    private Integer rssi;
    /**
     * Total acceleration
     */
    private Double accelerationTotal;
    /**
     * The angle between the acceleration vector and X axis
     */
    private Double accelerationAngleFromX;
    /**
     * The angle between the acceleration vector and Y axis
     */
    private Double accelerationAngleFromY;
    /**
     * The angle between the acceleration vector and Z axis
     */
    private Double accelerationAngleFromZ;
    /**
     * Absolute humidity in g/m^3
     */
    private Double absoluteHumidity;
    /**
     * Dew point in Celsius
     */
    private Double dewPoint;
    /**
     * Vapor pressure of water
     */
    private Double equilibriumVaporPressure;
    /**
     * Density of air
     */
    private Double airDensity;
    
    public Long getTime() {
        return time;
    }
    
    public void setTime(Long time) {
        this.time = time;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMac() {
        return mac;
    }
    
    public void setMac(String mac) {
        this.mac = mac;
    }
    
    public Integer getRssi() {
        return rssi;
    }
    
    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }
    
    public Double getAccelerationTotal() {
        return accelerationTotal;
    }
    
    public void setAccelerationTotal(Double accelerationTotal) {
        this.accelerationTotal = accelerationTotal;
    }
    
    public Double getAccelerationAngleFromX() {
        return accelerationAngleFromX;
    }
    
    public void setAccelerationAngleFromX(Double accelerationAngleFromX) {
        this.accelerationAngleFromX = accelerationAngleFromX;
    }
    
    public Double getAccelerationAngleFromY() {
        return accelerationAngleFromY;
    }
    
    public void setAccelerationAngleFromY(Double accelerationAngleFromY) {
        this.accelerationAngleFromY = accelerationAngleFromY;
    }
    
    public Double getAccelerationAngleFromZ() {
        return accelerationAngleFromZ;
    }
    
    public void setAccelerationAngleFromZ(Double accelerationAngleFromZ) {
        this.accelerationAngleFromZ = accelerationAngleFromZ;
    }
    
    public Double getAbsoluteHumidity() {
        return absoluteHumidity;
    }
    
    public void setAbsoluteHumidity(Double absoluteHumidity) {
        this.absoluteHumidity = absoluteHumidity;
    }
    
    public Double getDewPoint() {
        return dewPoint;
    }
    
    public void setDewPoint(Double dewPoint) {
        this.dewPoint = dewPoint;
    }
    
    public Double getEquilibriumVaporPressure() {
        return equilibriumVaporPressure;
    }
    
    public void setEquilibriumVaporPressure(Double equilibriumVaporPressure) {
        this.equilibriumVaporPressure = equilibriumVaporPressure;
    }
    
    public Double getAirDensity() {
        return airDensity;
    }
    
    public void setAirDensity(Double airDensity) {
        this.airDensity = airDensity;
    }

    @Override
    public String toString() {
        return "EnhancedRuuviMeasurement{" 
                + "time=" + time 
                + ", name=" + name 
                + ", mac=" + mac 
                + ", rssi=" + rssi 
                + ", accelerationTotal=" + accelerationTotal 
                + ", accelerationAngleFromX=" + accelerationAngleFromX 
                + ", accelerationAngleFromY=" + accelerationAngleFromY 
                + ", accelerationAngleFromZ=" + accelerationAngleFromZ 
                + ", absoluteHumidity=" + absoluteHumidity 
                + ", dewPoint=" + dewPoint 
                + ", equilibriumVaporPressure=" + equilibriumVaporPressure 
                + ", airDensity=" + airDensity 
                + ", super=" + super.toString()
                + '}';
    }
}
