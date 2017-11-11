package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DataFormatV5 implements BeaconHandler {

    private final int[] RUUVI_COPANY_IDENTIFIER = {0x99, 0x04}; // 0x0499
    /**
     * Contains the MAC address as key, and the timestamp of last sent update as
     * value
     */
    private final Map<String, Long> updatedMacs;
    private final long updateLimit = Config.getMeasurementUpdateLimit();

    public DataFormatV5() {
        updatedMacs = new HashMap<>();
    }

    @Override
    public RuuviMeasurement handle(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF);
        if (adData == null || !shouldUpdate(hciData.mac)) {
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

        m.temperature = (data[1] << 8 | data[2] & 0xFF) / 200d;

        m.humidity = ((data[3] & 0xFF) << 8 | data[4] & 0xFF) / 400d;

        m.pressure = (double) ((data[5] & 0xFF) << 8 | data[6] & 0xFF) + 50000;

        m.accelerationX = (data[7] << 8 | data[8] & 0xFF) / 1000d;
        m.accelerationY = (data[9] << 8 | data[10] & 0xFF) / 1000d;
        m.accelerationZ = (data[11] << 8 | data[12] & 0xFF) / 1000d;

        int powerInfo = (data[13] & 0xFF) << 8 | data[14] & 0xFF;
        m.batteryVoltage = (powerInfo >> 5) / 1000d + 1.6d;
        m.txPower = (powerInfo & 0b11111) * 2 - 40;

        m.movementCounter = data[15] & 0xFF;
        m.measurementSequenceNumber = (data[16] & 0xFF) << 8 | data[17] & 0xFF;

        return m;
    }

    private boolean shouldUpdate(String mac) {
        if (!Config.isAllowedMAC(mac)) {
            return false;
        }
        Long lastUpdate = updatedMacs.get(mac);
        if (lastUpdate == null || lastUpdate + updateLimit < System.currentTimeMillis()) {
            updatedMacs.put(mac, System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
