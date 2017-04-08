package fi.tkgwf.ruuvi.db;

import fi.tkgwf.ruuvi.bean.InfluxDBData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class InfluxDBConnection {

    private static final Logger LOG = Logger.getLogger(InfluxDBConnection.class);

    private final String influxUrl;

    public InfluxDBConnection(String influxUrl) {
        this.influxUrl = influxUrl;
    }

    public boolean post(InfluxDBData measurement) {
        System.out.println("::::");
        System.out.println(measurement);
        System.out.println("::__");
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(influxUrl).openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.connect();

            try (OutputStream os = con.getOutputStream()) {
                os.write(measurement.toString().getBytes("UTF-8"));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining("\n"));
                if (StringUtils.isNotBlank(response)) {
                    LOG.info("InfluxDB responded with: " + response);
                }
            }

            return true;
        } catch (IOException ex) {
            LOG.error("Failed to post measurement to InfluxDB", ex);
            return false;
        }
    }
}
