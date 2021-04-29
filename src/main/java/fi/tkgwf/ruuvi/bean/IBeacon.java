// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.bean;

import java.util.UUID;

public class IBeacon {

    private UUID uuid;

    private Integer major;

    private Integer minor;

    private Byte signalPower;

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Byte getSignalPower() {
        return signalPower;
    }

    public void setSignalPower(Byte signalPower) {
        this.signalPower = signalPower;
    }
}
