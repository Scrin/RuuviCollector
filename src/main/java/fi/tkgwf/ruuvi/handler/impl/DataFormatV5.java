package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.utils.RuuviUtils;
import fi.tkgwf.ruuvi.bean.InfluxDBData;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import java.util.HashMap;
import java.util.Map;

public class DataFormatV5 implements BeaconHandler {

    private static final String SENSORTAG_BEGINS = "> 04 3E 2B 02 01 00 01 ";
    /**
     * Contains the MAC address as key, and the timestamp of last sent update as
     * value
     */
    private final Map<String, Long> updatedMacs;
    private final long updateLimit = Config.getInfluxUpdateLimit();
    private String latestMac = null;
    private String latestBeginning = null;

    public DataFormatV5() {
        updatedMacs = new HashMap<>();
    }

    @Override
    public InfluxDBData read(String rawLine, String mac) {
        if (latestMac == null && (rawLine.startsWith(SENSORTAG_BEGINS))) { // line with Ruuvi MAC
            latestMac = RuuviUtils.getMacFromLine(rawLine.substring(SENSORTAG_BEGINS.length()));
        } else if (latestMac != null && latestBeginning == null) {
            latestBeginning = rawLine;
        } else if (latestMac != null && latestBeginning != null) {
            try {
                if (shouldUpdate(latestMac)) {
                    return handleMeasurement(latestMac, latestBeginning, rawLine);
                }
            } finally {
                latestMac = null;
                latestBeginning = null;
            }
        }
        return null;
    }

    @Override
    public void reset() {
        latestMac = null;
    }

    private InfluxDBData handleMeasurement(String mac, String firstPart, String secondPart) {
        String rawLine = firstPart.trim() + ' ' + secondPart.trim();
        rawLine = rawLine.substring(rawLine.indexOf(' ') + 1, rawLine.lastIndexOf(' ')); // discard first and last byte
        byte[] data = RuuviUtils.hexToBytes(rawLine);
        if (data.length < 24 || data[0] != 5) {
            return null; // unknown type
        }

        String protocolVersion = String.valueOf(data[0]);

        double temperature = (data[1] << 8 | data[2] & 0xFF) / 200d;

        double humidity = ((data[3] & 0xFF) << 8 | data[4] & 0xFF) / 400d;

        int pressure = ((data[5] & 0xFF) << 8 | data[6] & 0xFF) + 50000;

        double accelX = (data[7] << 8 | data[8] & 0xFF) / 1000d;
        double accelY = (data[9] << 8 | data[10] & 0xFF) / 1000d;
        double accelZ = (data[11] << 8 | data[12] & 0xFF) / 1000d;
        double accelTotal = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);

        int powerInfo = (data[13] & 0xFF) << 8 | data[14] & 0xFF;
        double battery = (powerInfo >> 5) / 1000d + 1.6d;
        int txPower = (powerInfo & 0b11111) * 2 - 40;

        int movementCounter = data[15] & 0xFF;

        int sequenceNumber = (data[16] & 0xFF) << 8 | data[17] & 0xFF;

        InfluxDBData.Builder builder = new InfluxDBData.Builder().mac(mac).protocolVersion(protocolVersion)
                .measurement("temperature").value(temperature)
                .measurement("humidity").value(humidity)
                .measurement("pressure").value(pressure)
                .measurement("acceleration").tag("axis", "x").value(accelX)
                .measurement("acceleration").tag("axis", "y").value(accelY)
                .measurement("acceleration").tag("axis", "z").value(accelZ)
                .measurement("acceleration").tag("axis", "total").value(accelTotal)
                .measurement("batteryVoltage").value(battery)
                .measurement("txPower").value(txPower)
                .measurement("movementCounter").value(movementCounter)
                .measurement("sequenceNumber").value(sequenceNumber);
        return builder.build();
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
