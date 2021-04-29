// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.bean;

public class EddystoneUID {

    private Byte signalPower;

    private String namespaceID;

    private String instanceID;

    public Byte getSignalPower() {
        return signalPower;
    }

    public void setSignalPower(Byte signalPower) {
        this.signalPower = signalPower;
    }

    public String getNamespaceID() {
        return namespaceID;
    }

    public void setNamespaceID(String namespaceID) {
        this.namespaceID = namespaceID;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }
}
