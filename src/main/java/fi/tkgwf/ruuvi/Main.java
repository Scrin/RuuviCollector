package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV2;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV4;
import fi.tkgwf.ruuvi.utils.HCIParser;
import fi.tkgwf.ruuvi.utils.InfluxDataMigrator;
import fi.tkgwf.ruuvi.utils.MeasurementValueCalculator;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV5;
import fi.tkgwf.ruuvi.utils.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private final List<BeaconHandler> beaconHandlers = new LinkedList<>();

    public void initializeHandlers() {
        beaconHandlers.add(new DataFormatV2());
        beaconHandlers.add(new DataFormatV3());
        beaconHandlers.add(new DataFormatV4());
        beaconHandlers.add(new DataFormatV5());
    }

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("migrate")) {
            InfluxDataMigrator migrator = new InfluxDataMigrator();
            migrator.migrate();
        } else {
            Main m = new Main();
            m.initializeHandlers();
            if (!m.run()) {
                System.exit(1);
            }
        }
        LOG.info("Clean exit");
        System.exit(0); // due to a bug in the InfluxDB library, we have to force the exit as a workaround. See: https://github.com/influxdata/influxdb-java/issues/359
    }

    private BufferedReader startHciListeners() throws IOException {
        String[] scan = Config.getScanCommand();
        if (scan.length > 0 && StringUtils.isNotBlank(scan[0])) {
            Process hcitool = new ProcessBuilder(scan).start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> hcitool.destroyForcibly()));
            LOG.debug("Starting scan with: " + Arrays.toString(scan));
        } else {
            LOG.debug("Skipping scan command, scan command is blank.");
        }
        Process hcidump = new ProcessBuilder(Config.getDumpCommand()).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> hcidump.destroyForcibly()));
        LOG.debug("Starting dump with: " + Arrays.toString(Config.getDumpCommand()));
        return new BufferedReader(new InputStreamReader(hcidump.getInputStream()));
    }

    /**
     * Run the collector.
     *
     * @return true if the run ends gracefully, false in case of severe errors
     */
    private boolean run() {
        BufferedReader reader;
        try {
            reader = startHciListeners();
        } catch (IOException ex) {
            LOG.error("Failed to start hci processes", ex);
            return false;
        }
        LOG.info("BLE listener started successfully, waiting for data... \nIf you don't get any data, check that you are able to run 'hcitool lescan' and 'hcidump --raw' without issues");
        DBConnection db = Config.getDBConnection();
        HCIParser parser = new HCIParser();
        boolean dataReceived = false;
        try {
            String line, latestMAC = null;
            while ((line = reader.readLine()) != null) {
                if (!dataReceived) {
                    if (line.startsWith("> ")) {
                        LOG.info("Successfully reading data from hcidump");
                        dataReceived = true;
                    } else {
                        continue; // skip the unnecessary garbage at beginning containing hcidump version and other junk print
                    }
                }
                try {
                    if (line.startsWith("> ")) {
                        latestMAC = Utils.getMacFromLine(line.substring(23));
                    }
                    HCIData hciData = parser.readLine(line);
                    if (hciData != null) {
                        beaconHandlers.stream()
                                .map(handler -> handler.handle(hciData))
                                .filter(Objects::nonNull)
                                .map(MeasurementValueCalculator::calculateAllValues)
                                .forEach(db::save);
                    }
                } catch (Exception ex) {
                    LOG.warn("Uncaught exception while handling measurements from MAC address \"" + latestMAC + "\", if this repeats and this is not a Ruuvitag, consider blacklisting it", ex);
                    beaconHandlers.forEach(BeaconHandler::reset);
                }
            }
        } catch (IOException ex) {
            LOG.error("Uncaught exception while reading measurements", ex);
            return false;
        } finally {
            db.close();
        }
        return true;
    }
}
