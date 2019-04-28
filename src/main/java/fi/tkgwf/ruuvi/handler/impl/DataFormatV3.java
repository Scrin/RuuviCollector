package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandlerInterface;
import java.util.Arrays;

public class DataFormatV3 implements BeaconHandlerInterface {

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
        if (data.length < 14 || data[0] != 3) {
            return null;
        }
        RuuviMeasurement m = new RuuviMeasurement();
        m.mac = hciData.mac;
        m.rssi = hciData.rssi;
        m.dataFormat = data[0] & 0xFF;

        m.humidity = ((double) (data[1] & 0xFF)) / 2d;

        int temperatureSign = (data[2] >> 7) & 1;
        int temperatureBase = (data[2] & 0x7F);
        double temperatureFraction = ((float) data[3]) / 100d;
        m.temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            m.temperature *= -1;
        }

        int pressureHi = data[4] & 0xFF;
        int pressureLo = data[5] & 0xFF;
        m.pressure = (double) pressureHi * 256 + 50000 + pressureLo;

        m.accelerationX = (data[6] << 8 | data[7] & 0xFF) / 1000d;
        m.accelerationY = (data[8] << 8 | data[9] & 0xFF) / 1000d;
        m.accelerationZ = (data[10] << 8 | data[11] & 0xFF) / 1000d;

        int battHi = data[12] & 0xFF;
        int battLo = data[13] & 0xFF;
        m.batteryVoltage = (battHi * 256 + battLo) / 1000d;
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
        if (data.length < 14 || data[0] != 3) {
            return false;
        }
        return true;
    }

}
