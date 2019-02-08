package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;

public abstract class AbstractEddystoneURL implements BeaconHandler {

    private static final String RUUVI_BASE_URL = "ruu.vi/#";

    abstract protected byte[] base64ToByteArray(String base64);

    @Override
    public RuuviMeasurement handle(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0x16);
        if (adData == null || !Config.isAllowedMAC(hciData.mac)) {
            return null;
        }
        String hashPart = getRuuviUrlHashPart(adData.dataBytes());
        if (hashPart == null) {
            return null; // not a ruuvi url
        }
        byte[] data;
        try {
            data = base64ToByteArray(hashPart);
        } catch (IllegalArgumentException ex) {
            return null; // V2 format will throw this when trying to parse V4 and vice versa
        }
        if (data.length < 6 || data[0] != 2 && data[0] != 4) {
            return null; // unknown type
        }
        RuuviMeasurement measurement = new RuuviMeasurement();
        measurement.mac = hciData.mac;
        measurement.rssi = hciData.rssi;
        measurement.dataFormat = data[0] & 0xFF;

        measurement.humidity = ((double) (data[1] & 0xFF)) / 2d;

        int temperatureSign = (data[2] >> 7) & 1;
        int temperatureBase = (data[2] & 0x7F);
        double temperatureFraction = ((float) data[3]) / 100d;
        measurement.temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            measurement.temperature *= -1;
        }

        int pressureHi = data[4] & 0xFF;
        int pressureLo = data[5] & 0xFF;
        measurement.pressure = (double) pressureHi * 256 + 50000 + pressureLo;
        return measurement;
    }

    @Override
    public boolean canHandle(HCIData hciData) {
        final HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0x16);
        return adData != null && getRuuviUrlHashPart(adData.dataBytes()) != null;
    }

    private String getRuuviUrlHashPart(byte[] data) {
        if (data.length < 15) {
            return null; // too short
        }
        if ((data[0] & 0xFF) != 0xAA && (data[1] & 0xFF) != 0xFE) {
            return null; // not an eddystone UUID
        }
        if (data[2] != 0x10) {
            return null; // not an eddystone URL
        }
        if (data[4] != 0x03) {
            return null; // not https://
        }
        String basePart = new String(data, 5, data.length - (5));
        if (!basePart.startsWith(RUUVI_BASE_URL)) {
            return null; // not a ruuvi url
        }
        int preLength = 5 + RUUVI_BASE_URL.length();
        return new String(data, preLength, data.length - preLength);
    }

}
