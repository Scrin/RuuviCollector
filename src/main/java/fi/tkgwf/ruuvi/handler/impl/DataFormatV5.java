package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandlerInterface;
import fi.tkgwf.ruuvi.utils.Utils;

import java.util.Arrays;

public class DataFormatV5 implements BeaconHandlerInterface {

    private final int[] RUUVI_COPANY_IDENTIFIER = {0x99, 0x04}; // 0x0499

    @Override
    public RuuviMeasurement handle(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF);
        if (adData == null || !Config.isAllowedMAC(hciData.mac)) {
            return null;
        }
        byte[] data = adData.dataBytes();
        if (data.length < 2 || (data[0] & 0xFF) != RUUVI_COPANY_IDENTIFIER[0] || (data[1] & 0xFF) != RUUVI_COPANY_IDENTIFIER[1]) {
            return null;
        }
        data = Arrays.copyOfRange(data, 2, data.length); // discard the first 2 bytes, the company identifier
        if (data.length < 24 || data[0] != 5) {
            return null;
        }
        RuuviMeasurement m = new RuuviMeasurement();
        m.mac = hciData.mac;
        m.rssi = hciData.rssi;
        m.dataFormat = data[0] & 0xFF;

        if (!Utils.isMaxSignedShort(data[1], data[2])) {
            m.temperature = (data[1] << 8 | data[2] & 0xFF) / 200d;
        }

        if (!Utils.isMaxUnsignedShort(data[3], data[4])) {
            m.humidity = ((data[3] & 0xFF) << 8 | data[4] & 0xFF) / 400d;
        }

        if (!Utils.isMaxUnsignedShort(data[5], data[6])) {
            m.pressure = (double) ((data[5] & 0xFF) << 8 | data[6] & 0xFF) + 50000;
        }

        if (!Utils.isMaxSignedShort(data[7], data[8])) {
            m.accelerationX = (data[7] << 8 | data[8] & 0xFF) / 1000d;
        }
        if (!Utils.isMaxSignedShort(data[9], data[10])) {
            m.accelerationY = (data[9] << 8 | data[10] & 0xFF) / 1000d;
        }
        if (!Utils.isMaxSignedShort(data[11], data[2])) {
            m.accelerationZ = (data[11] << 8 | data[12] & 0xFF) / 1000d;
        }

        int powerInfo = (data[13] & 0xFF) << 8 | data[14] & 0xFF;
        if ((powerInfo >>> 5) != 0b11111111111) {
            m.batteryVoltage = (powerInfo >>> 5) / 1000d + 1.6d;
        }
        if ((powerInfo & 0b11111) != 0b11111) {
            m.txPower = (powerInfo & 0b11111) * 2 - 40;
        }

        if (!Utils.isMaxUnsignedByte(data[15])) {
            m.movementCounter = data[15] & 0xFF;
        }
        if (!Utils.isMaxSignedShort(data[16], data[17])) {
            m.measurementSequenceNumber = (data[16] & 0xFF) << 8 | data[17] & 0xFF;
        }

        return m;
    }

    @Override
    public boolean canHandle(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF);
        if (adData == null) {
            return false;
        }
        byte[] data = adData.dataBytes();
        if (data.length < 2 || (data[0] & 0xFF) != RUUVI_COPANY_IDENTIFIER[0] || (data[1] & 0xFF) != RUUVI_COPANY_IDENTIFIER[1]) {
            return false;
        }
        data = Arrays.copyOfRange(data, 2, data.length); // discard the first 2 bytes, the company identifier
        if (data.length < 24 || data[0] != 5) {
            return false;
        }
        return true;
    }

}
