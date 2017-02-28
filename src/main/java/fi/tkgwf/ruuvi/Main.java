package fi.tkgwf.ruuvi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class Main {

    public static final String RUUVI_BEGINS = "> 04 3E 2A 02 01 03 01 ";
    public static final String RUUVI_URL = " 72 75 75 2E 76 69 2F 23 ";

    // TODO not hardcoded...
    private static String influxUrlBase = "http://localhost:8806";
    private static String influxDatabase = "ruuvi";
    private static String influxUser = "ruuvi";
    private static String influxPassword = "ruuvi";
    // TODO less static...
    private static String influxUrl = String.format("%s/write?db=%s&u=%s&p=%s", influxUrlBase, influxDatabase, influxUser, influxPassword);

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
            String line, latestMac = null, latestUrl = null;
            while ((line = reader.readLine()) != null) {
                try {
                    if (latestMac == null && latestUrl == null && line.startsWith(RUUVI_BEGINS)) { // line with Ruuvi MAC
                        latestMac = getMacFromLine(line);
                    } else if (latestMac != null && latestUrl == null && line.contains(RUUVI_URL)) { // revious line had a Ruuvi MAC, this has beginning of url
                        latestUrl = getRuuviUrlBeginningFromLine(line);
                    } else if (latestMac != null && latestUrl != null) { // this has the remaining part of the url
                        latestUrl += getRuuviUrlEndingFromLine(line);
                        handleMeasurement(latestMac, hexToAscii(latestUrl));
                        latestMac = null;
                        latestUrl = null;
                    }
                } catch (Exception ex) {
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

    private static void handleMeasurement(String mac, String base64) {
        byte[] data = Base64.getDecoder().decode(base64);
        float humidity = ((float) (data[1] & 0xFF)) / 2f;
        int temperatureSign = (data[2] >> 7) & 1;
        int temperatureBase = (data[2] & 0x7F);
        float temperatureFraction = ((float) data[3]) / 100f;
        int pressureHi = data[4] & 0xFF;
        int pressureLo = data[5] & 0xFF;
        float temperature = ((float) temperatureBase) + temperatureFraction;
        if (temperatureSign == 1) {
            temperature *= -1;
        }
        int pressure = pressureHi * 256 + 50000 + pressureLo;
//        System.out.println("MAC: " + mac + " Temp: " + temperature + " Humidity: " + humidity + " Pressure: " + pressure);
        StringBuilder sb = new StringBuilder();
        sb.append("temperature,source=").append(mac).append(" value=").append(temperature).append("\n");
        sb.append("humidity,source=").append(mac).append(" value=").append(humidity).append("\n");
        sb.append("pressure,source=").append(mac).append(" value=").append(pressure).append("\n");
        post(sb.toString());
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

    private static String getRuuviUrlBeginningFromLine(String line) {
        return line.substring(line.indexOf(RUUVI_URL) + RUUVI_URL.length());
    }

    private static String getRuuviUrlEndingFromLine(String line) {
        line = line.trim();
        return line.substring(0, line.lastIndexOf(' '));
    }

    private static String getMacFromLine(String line) {
        StringBuilder sb = new StringBuilder();
        line = line.substring(RUUVI_BEGINS.length());
        String[] split = line.split(" ", 7); // 6 blocks plus remaining garbage
        for (int i = 5; i >= 0; i--) {
            sb.append(split[i]);
        }
        return sb.toString();
    }
}
