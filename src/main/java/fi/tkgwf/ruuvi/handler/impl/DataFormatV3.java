package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.utils.RuuviUtils;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import java.util.HashMap;
import java.util.Map;

public class DataFormatV3 implements BeaconHandler {

    // For some reason the latest sensortag data format has four null bytes at the end, changing the length of the raw packer (third byte is the length)
    private static final String OLDER_SENSORTAG_BEGINS = "> 04 3E 21 02 01 03 01 ";
    private static final String SENSORTAG_BEGINS = "> 04 3E 25 02 01 03 01 ";
    /**
     * Contains the MAC address as key, and the timestamp of last sent update as
     * value
     */
    private final Map<String, Long> updatedMacs;
    private final long updateLimit = Config.getInfluxUpdateLimit();
    private String latestMac = null;

    public DataFormatV3() {
        updatedMacs = new HashMap<>();
    }

    @Override
    public RuuviMeasurement read(String rawLine, String mac) {
        if (latestMac == null && (rawLine.startsWith(SENSORTAG_BEGINS) || rawLine.startsWith(OLDER_SENSORTAG_BEGINS))) { // line with Ruuvi MAC
            latestMac = RuuviUtils.getMacFromLine(rawLine.substring(SENSORTAG_BEGINS.length()));
        } else if (latestMac != null) {
            try {
                if (shouldUpdate(latestMac)) {
                    return handleMeasurement(latestMac, rawLine);
                }
            } finally {
                latestMac = null;
            }
        }
        return null;
    }

    @Override
    public void reset() {
        latestMac = null;
    }

    private RuuviMeasurement handleMeasurement(String mac, String rawLine) {
        rawLine = rawLine.trim(); // trim whitespace
        rawLine = rawLine.substring(rawLine.indexOf(' ') + 1, rawLine.lastIndexOf(' ')); // discard first and last byte
        byte[] data = RuuviUtils.hexToBytes(rawLine);
        if (data.length < 14 || data[0] != 3) {
            return null; // unknown type
        }
        int protocolVersion = data[0] & 0xFF;

        double humidity = ((float) (data[1] & 0xFF)) / 2f;

        int temperatureSign = (data[2] >> 7) & 1;
        int temperatureBase = (data[2] & 0x7F);
        double temperatureFraction = ((float) data[3]) / 100f;
        double temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            temperature *= -1;
        }

        int pressureHi = data[4] & 0xFF;
        int pressureLo = data[5] & 0xFF;
        double pressure = pressureHi * 256 + 50000 + pressureLo;

        double accelX = (data[6] << 8 | data[7] & 0xFF) / 1000f;
        double accelY = (data[8] << 8 | data[9] & 0xFF) / 1000f;
        double accelZ = (data[10] << 8 | data[11] & 0xFF) / 1000f;
        double accelTotal = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);

        int battHi = data[12] & 0xFF;
        int battLo = data[13] & 0xFF;
        double battery = (battHi * 256 + battLo) / 1000f;

        // TODO: Refactor and remove the unnecessary temp variables above
        RuuviMeasurement measurement = new RuuviMeasurement();
        measurement.mac = mac;
        measurement.dataFormat = protocolVersion;
        measurement.temperature = temperature;
        measurement.relativeHumidity = humidity;
        measurement.pressure = pressure;
        measurement.accelerationX = accelX;
        measurement.accelerationY = accelY;
        measurement.accelerationZ = accelZ;
        measurement.accelerationTotal = accelTotal;
        measurement.batteryVoltage = battery;
        return measurement;
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
