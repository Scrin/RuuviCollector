package fi.tkgwf.ruuvi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static final String SENSORTAG_BEGINS = "> 04 3E 21 02 01 03 01 ";
    public static final String RUUVI_BEGINS = "> 04 3E 2A 02 01 03 01 ";
    public static final String RUUVI_URL = " 72 75 75 2E 76 69 2F 23 ";

    public static final long INFLUX_UPDATE_LIMIT_MS = 9900; // 9.9 sec

    // TODO not hardcoded...
    private static String influxUrlBase = "http://localhost:8086";
    private static String influxDatabase = "ruuvi";
    private static String influxUser = "ruuvi";
    private static String influxPassword = "ruuvi";
    // TODO less static...
    private static String influxUrl = String.format("%s/write?db=%s&u=%s&p=%s", influxUrlBase, influxDatabase, influxUser, influxPassword);

    /**
     * Contains the MAC address as key, and the timestamp of last sent update as
     * value
     */
    private static Map<String, Long> updatedMacs = new HashMap<>();

    public static void main(String[] args) {
        ProcessBuilder hcitoolBuilder = new ProcessBuilder("hcitool", "lescan", "--duplicates");
        ProcessBuilder hcidumpBuilder = new ProcessBuilder("hcidump", "--raw");
        try {
            Process hcitool = hcitoolBuilder.start();
            Process hcidump = hcidumpBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                hcitool.destroyForcibly();
                hcidump.destroyForcibly();
            }));
            BufferedReader reader = new BufferedReader(new InputStreamReader(hcidump.getInputStream()));

            //TODO cleanup this horror
            String line, latestMac = null, latestUrl = null;
            boolean traditionalRuuvi = false, sensortag = false;
            while ((line = reader.readLine()) != null) {
                try {
                    if (!sensortag) { // currently not reading data from a sensortag fw
                        if (latestMac == null && latestUrl == null && line.startsWith(RUUVI_BEGINS)) { // line with Ruuvi MAC
                            latestMac = getMacFromLine(line, RUUVI_BEGINS);
                            traditionalRuuvi = true;
                        } else if (latestMac != null && latestUrl == null && line.contains(RUUVI_URL)) { // revious line had a Ruuvi MAC, this has beginning of url
                            latestUrl = getRuuviUrlBeginningFromLine(line);
                        } else if (latestMac != null && latestUrl != null) { // this has the remaining part of the url
                            latestUrl += getRuuviUrlEndingFromLine(line);
                            handleTraditionalRuuviMeasurement(latestMac, hexToAscii(latestUrl));
                            latestMac = null;
                            latestUrl = null;
                            traditionalRuuvi = false;
                        }
                    }
                    if (!traditionalRuuvi) { // currently not reading data from a ruuvi
                        if (latestMac == null && line.startsWith(SENSORTAG_BEGINS)) { // line with Ruuvi MAC
                            latestMac = getMacFromLine(line, RUUVI_BEGINS);
                            sensortag = true;
                        } else if (latestMac != null) {
                            handleSensortagMeasurement(latestMac, line);
                            latestMac = null;
                            sensortag = false;
                        }
                    }
                } catch (Exception ex) {
                    latestMac = null;
                    latestUrl = null;
                    traditionalRuuvi = false;
                    sensortag = false;
                    System.err.println("Exception caught!");
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            System.err.println("Process failed");
            ex.printStackTrace();
            System.exit(1);
        }
        System.out.println("Quit");
    }

    private static void handleSensortagMeasurement(String mac, String hex) {
        if (!shouldUpdate(mac)) {
            return;
        }
        /*
        Offset  Allowed values              Description
        0       3                           Data format definition (3 = current sensor readings)
        1       0 ... 200                   Humidity (one lsb is 0.5%, e.g. 128 is 64%)
        2       -127 ... 127, signed        Temperature (MSB is sign, next 7 bits are decimal value)
        3       0 ... 99                    Temperature (fraction, 1/100.)
        4  - 5	0 ... 65535                 Pressure (Most Significant Byte first, value - 50kPa)
        6  - 7	-32767 ... 32767, signed    Acceleration-X (Most Significant Byte first)
        8  - 9	-32767 ... 32767, signed    Acceleration-Y (Most Significant Byte first)
        10 - 11	-32767 ... 32767, signed    Acceleration-Z (Most Significant Byte first)
        12 - 13	0 ... 65535                 Battery voltage (millivolts). MSB First
         */
        hex = hex.trim(); // trim whitespace
        hex = hex.substring(hex.indexOf(' ') + 1, hex.lastIndexOf(' ')); // discard first and last byte
//        int[] data = hexToDec(hex);
        byte[] data = hexToBytes(hex);
        if (data[0] != 3) {
            return; // unknown type
        }
        
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

//        System.out.println("Sensortag FW: MAC: " + mac
//                + " Temp: " + temperature
//                + " Humidity: " + humidity
//                + " Pressure: " + pressure
//                + " Accel X: " + accelX
//                + " Accel Y: " + accelY
//                + " Accel Z: " + accelZ
//                + " Accel Total: " + accelTotal
//                + " Battery: " + battery);
        
        StringBuilder sb = new StringBuilder();
        sb.append("temperature,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(temperature).append("\n");
        sb.append("humidity,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(humidity).append("\n");
        sb.append("pressure,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(pressure).append("\n");
        sb.append("accelerationX,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(accelX).append("\n");
        sb.append("accelerationY,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(accelY).append("\n");
        sb.append("accelerationZ,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(accelZ).append("\n");
        sb.append("accelerationTotal,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(accelTotal).append("\n");
        sb.append("batteryVoltage,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(battery).append("\n");
        post(sb.toString());
    }

    private static void handleTraditionalRuuviMeasurement(String mac, String base64) {
        if (!shouldUpdate(mac)) {
            return;
        }
        byte[] data = Base64.getDecoder().decode(base64.replace('-', '+').replace('_', '/')); // Ruuvi uses URL-safe Base64, convert that to "traditional" Base64
        if (data[0] != 2) {
            return; // unknown type
        }
        
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
        
//        System.out.println("Traditional Ruuvi FW: MAC: " + mac + " Temp: " + temperature + " Humidity: " + humidity + " Pressure: " + pressure);
        
        StringBuilder sb = new StringBuilder();
        sb.append("temperature,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(temperature).append("\n");
        sb.append("humidity,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(humidity).append("\n");
        sb.append("pressure,protocolVersion=").append(data[0]).append(",source=").append(mac).append(" value=").append(pressure).append("\n");
        post(sb.toString());
    }

    private static boolean shouldUpdate(String mac) {
        Long lastUpdate = updatedMacs.get(mac);
        if (lastUpdate == null || lastUpdate + INFLUX_UPDATE_LIMIT_MS < System.currentTimeMillis()) {
            updatedMacs.put(mac, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private static void post(String data) {
        try {
            HttpURLConnection httpcon = (HttpURLConnection) new URL(influxUrl).openConnection();
            httpcon.setDoOutput(true);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("POST");
            httpcon.connect();

            byte[] outputBytes = data.getBytes("UTF-8");
            OutputStream os = httpcon.getOutputStream();
            os.write(outputBytes);
            os.close();
            new BufferedReader(new InputStreamReader(httpcon.getInputStream())).lines().forEach(l -> System.out.println("RESP: " + l));
        } catch (IOException ex) {
            System.err.println("Failed to post to InfluxDB!");
            ex.printStackTrace();
        }

    }

    private static String hexToAscii(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 3) {
            sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        String s = hex.replaceAll(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String getRuuviUrlBeginningFromLine(String line) {
        return line.substring(line.indexOf(RUUVI_URL) + RUUVI_URL.length());
    }

    private static String getRuuviUrlEndingFromLine(String line) {
        line = line.trim();
        return line.substring(0, line.lastIndexOf(' '));
    }

    private static String getMacFromLine(String line, String prefix) {
        StringBuilder sb = new StringBuilder();
        line = line.substring(prefix.length());
        String[] split = line.split(" ", 7); // 6 blocks plus remaining garbage
        for (int i = 5; i >= 0; i--) {
            sb.append(split[i]);
        }
        return sb.toString();
    }
}
