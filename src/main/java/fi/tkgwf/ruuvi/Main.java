package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV2;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private final List<BeaconHandler> beaconHandlers = new LinkedList<>();

    public void initializeHandlers(long influxUpdateLimit) {
        beaconHandlers.add(new DataFormatV2(influxUpdateLimit));
        beaconHandlers.add(new DataFormatV3(influxUpdateLimit));
    }

    public static void main(String[] args) {
        // Defaults..
        String influxUrlBase = "http://localhost:8086";
        String influxDatabase = "ruuvi";
        String influxUser = "ruuvi";
        String influxPassword = "ruuvi";
        long influxUpdateLimit = 9900;

        // Read config..
        try {
            File jarLocation = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
            File[] configFiles = jarLocation.listFiles(f -> f.isFile() && f.getName().equals("ruuvi-collector.properties"));
            if (configFiles == null || configFiles.length == 0) {
                // look for config files in the parent directory if none found in the current directory, this is useful during development when
                // RuuviCollector can be run from maven target directory directly while the config file sits in the project root
                configFiles = jarLocation.getParentFile().listFiles(f -> f.isFile() && f.getName().equals("ruuvi-collector.properties"));
            }
            if (configFiles != null && configFiles.length > 0) {
                LOG.debug("Config: " + configFiles[0]);
                Properties props = new Properties();
                props.load(new FileInputStream(configFiles[0]));
                Enumeration<?> e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = props.getProperty(key);
                    switch (key) {
                        case "influxUrlBase":
                            influxUrlBase = value;
                            break;
                        case "influxDatabase":
                            influxDatabase = value;
                            break;
                        case "influxUser":
                            influxUser = value;
                            break;
                        case "influxPassword":
                            influxPassword = value;
                            break;
                        case "influxUpdateLimit":
                            try {
                                influxUpdateLimit = Long.parseLong(value);
                            } catch (NumberFormatException ex) {
                                LOG.warn("Malformed number format for influxUpdateLimit: '" + value + '\'');
                            }
                            break;
                    }
                }
            }
        } catch (URISyntaxException | IOException ex) {
            LOG.warn("Failed to read configuration, using default values...", ex);
        }
        String influxUrl = String.format("%s/write?db=%s&u=%s&p=%s", influxUrlBase, influxDatabase, influxUser, influxPassword);

        // Start the collector..
        Main m = new Main();
        m.initializeHandlers(influxUpdateLimit);
        if (!m.run(influxUrl)) {
            System.exit(1);
        }
    }

    private BufferedReader startHciListeners() throws IOException {
        Process hcitool = new ProcessBuilder("hcitool", "lescan", "--duplicates").start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hcitool.destroyForcibly()));
        Process hcidump = new ProcessBuilder("hcidump", "--raw").start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hcidump.destroyForcibly()));
        return new BufferedReader(new InputStreamReader(hcidump.getInputStream()));
    }

    private boolean run(String influxUrl) {
        InfluxDBConnection influx = new InfluxDBConnection(influxUrl);
        BufferedReader reader;
        try {
            reader = startHciListeners();
        } catch (IOException ex) {
            LOG.error("Failed to start hci processes", ex);
            return false;
        }
        LOG.info("BLE listener started successfully, waiting for data... \nIf you don't get any data, check that you are able to run 'hcitool lescan --duplicates' and 'hcidump --raw' without issues");
        boolean dataReceived = false;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!dataReceived) {
                    LOG.info("Successfully reading data from hcidump");
                    dataReceived = true;
                }
                try {
                    String finalLine = line; // lambdas require this to be effectively final
                    beaconHandlers.stream()
                            .map(handler -> handler.read(finalLine))
                            .filter(measurement -> measurement != null)
                            .forEach(influx::post);
                } catch (Exception ex) {
                    LOG.warn("Uncaught exception while handling measurements", ex);
                    beaconHandlers.forEach(BeaconHandler::reset);
                }
            }
        } catch (IOException ex) {
            LOG.error("Uncaught exception while reading measurements", ex);
            return false;
        }
        return true;
    }
}
