package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.utils.RuuviUtils;
import fi.tkgwf.ruuvi.bean.InfluxDBData;
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
    private final long updateLimit;
    private String latestMac = null;

    public DataFormatV3(long updateLimit) {
        updatedMacs = new HashMap<>();
        this.updateLimit = updateLimit;
    }

    @Override
    public InfluxDBData read(String rawLine) {
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

    private InfluxDBData handleMeasurement(String mac, String rawLine) {
        rawLine = rawLine.trim(); // trim whitespace
        rawLine = rawLine.substring(rawLine.indexOf(' ') + 1, rawLine.lastIndexOf(' ')); // discard first and last byte
        byte[] data = RuuviUtils.hexToBytes(rawLine);
        if (data[0] != 3) {
            return null; // unknown type
        }
        String protocolVersion = String.valueOf(data[0]);

        float humidity = ((float) (data[1] & 0xFF)) / 2f;

        int temperatureSign = (data[2] >> 7) & 1;
        int temperatureBase = (data[2] & 0x7F);
        float temperatureFraction = ((float) data[3]) / 100f;
        float temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            temperature *= -1;
        }

        int pressureHi = data[4] & 0xFF;
        int pressureLo = data[5] & 0xFF;
        int pressure = pressureHi * 256 + 50000 + pressureLo;

        float accelX = (data[6] << 8 | data[7] & 0xFF) / 1000f;
        float accelY = (data[8] << 8 | data[9] & 0xFF) / 1000f;
        float accelZ = (data[10] << 8 | data[11] & 0xFF) / 1000f;
        double accelTotal = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);

        int battHi = data[12] & 0xFF;
        int battLo = data[13] & 0xFF;
        float battery = (battHi * 256 + battLo) / 1000f;

        InfluxDBData.Builder builder = new InfluxDBData.Builder().mac(mac).protocolVersion(protocolVersion)
                .measurement("temperature").value(temperature)
                .measurement("humidity").value(humidity)
                .measurement("pressure").value(pressure)
                .measurement("acceleration").tag("axis", "x").value(accelX)
                .measurement("acceleration").tag("axis", "y").value(accelY)
                .measurement("acceleration").tag("axis", "z").value(accelZ)
                .measurement("acceleration").tag("axis", "total").value(accelTotal)
                .measurement("batteryVoltage").value(battery);
        return builder.build();
    }

    private boolean shouldUpdate(String mac) {
        Long lastUpdate = updatedMacs.get(mac);
        if (lastUpdate == null || lastUpdate + updateLimit < System.currentTimeMillis()) {
            updatedMacs.put(mac, System.currentTimeMillis());
            return true;
        }
        return false;
    }
}
