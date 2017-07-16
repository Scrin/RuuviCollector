package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.bean.InfluxDBData;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.utils.RuuviUtils;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEddystoneURL implements BeaconHandler {
    
    private static final String RUUVI_URL = " 72 75 75 2E 76 69 2F 23 ";

    private final Map<String, Long> updatedMacs;
    private final long updateLimit = Config.getInfluxUpdateLimit();
    private String latestMac = null;
    private String latestUrlBeginning = null;

    public AbstractEddystoneURL() {
        updatedMacs = new HashMap<>();
    }
    
    abstract protected String getRuuviBegins();

    @Override
    public InfluxDBData read(String rawLine, String mac) {
        if (latestMac == null && latestUrlBeginning == null && rawLine.startsWith(getRuuviBegins())) { // line with Ruuvi MAC
            latestMac = RuuviUtils.getMacFromLine(rawLine.substring(getRuuviBegins().length()));
        } else if (latestMac != null && latestUrlBeginning == null && rawLine.contains(RUUVI_URL)) { // revious line had a Ruuvi MAC, this has beginning of url
            latestUrlBeginning = getRuuviUrlBeginningFromLine(rawLine);
        } else if (latestMac != null && latestUrlBeginning != null) { // this has the remaining part of the url
            try {
                if (shouldUpdate(latestMac)) {
                    String url = latestUrlBeginning + getRuuviUrlEndingFromLine(rawLine);
                    return handleMeasurement(latestMac, RuuviUtils.hexToAscii(url));
                }
            } finally {
                latestMac = null;
                latestUrlBeginning = null;
            }
        }
        return null;
    }

    @Override
    public void reset() {
        latestMac = null;
        latestUrlBeginning = null;
    }
    
    protected byte[] base64ToByteArray(String base64){
        return Base64.getDecoder().decode(base64.replace('-', '+').replace('_', '/')); // Ruuvi uses URL-safe Base64, convert that to "traditional" Base64
    }

    private InfluxDBData handleMeasurement(String mac, String base64) {
        byte[] data = base64ToByteArray(base64);
        if (data.length < 6 || data[0] != 2 && data[0] != 4) {
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

        InfluxDBData.Builder builder = new InfluxDBData.Builder().mac(mac).protocolVersion(protocolVersion)
                .measurement("temperature").value(temperature)
                .measurement("humidity").value(humidity)
                .measurement("pressure").value(pressure);
        return builder.build();
    }

    private String getRuuviUrlBeginningFromLine(String line) {
        return line.substring(line.indexOf(RUUVI_URL) + RUUVI_URL.length());
    }

    private String getRuuviUrlEndingFromLine(String line) {
        line = line.trim();
        return line.substring(0, line.lastIndexOf(' '));
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
