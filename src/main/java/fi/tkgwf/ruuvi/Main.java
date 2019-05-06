package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.service.PersistenceService;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.utils.HCIParser;
import fi.tkgwf.ruuvi.utils.InfluxDataMigrator;
import fi.tkgwf.ruuvi.utils.MeasurementValueCalculator;
import fi.tkgwf.ruuvi.utils.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private final BeaconHandler beaconHandler = new BeaconHandler();

    public static void main(String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("migrate")) {
            InfluxDataMigrator migrator = new InfluxDataMigrator();
            migrator.migrate();
        } else {
            Main m = new Main();
            if (!m.run()) {
                LOG.info("Unclean exit");
                System.exit(1);
            }
        }
        LOG.info("Clean exit");
        System.exit(0); // due to a bug in the InfluxDB library, we have to force the exit as a
                        // workaround. See: https://github.com/influxdata/influxdb-java/issues/359
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
    public boolean run() {
        BufferedReader reader;
        try {
            reader = startHciListeners();
        } catch (IOException ex) {
            LOG.error("Failed to start hci processes", ex);
            return false;
        }
        LOG.info("BLE listener started successfully, waiting for data... \n " +
                "If you don't get any data, check that you are able to run 'hcitool lescan' and 'hcidump --raw' without issues");
        return run(reader);
    }

    boolean run(final BufferedReader reader) {
        HCIParser parser = new HCIParser();
        boolean dataReceived = false;
        boolean healthy = false;
        try (final PersistenceService persistenceService = new PersistenceService()) {
            String line, latestMAC = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("device: disconnected")) {
                    LOG.error(line + ": Either the bluetooth device was externally disabled or physically disconnected");
                    healthy = false;
                }
                if (line.contains("No such device")) {
                    LOG.error(line + ": Check that your bluetooth adapter is enabled and working properly");
                    healthy = false;
                }
                if (!dataReceived) {
                    if (line.startsWith("> ")) {
                        LOG.info("Successfully reading data from hcidump");
                        dataReceived = true;
                        healthy = true;
                    } else {
                        continue; // skip the unnecessary garbage at beginning containing hcidump version and other junk print
                    }
                }
                try {
                    //Read in MAC address from first line
                    if (Utils.hasMacAddress(line)) {
                        latestMAC = Utils.getMacFromLine(line.substring(23));
                    }
                    //Apply Mac Address Filtering
                    if (Config.isAllowedMAC(latestMAC)) {
                        HCIData hciData = parser.readLine(line);
                        if (hciData != null) {
                            beaconHandler.handle(hciData).map(MeasurementValueCalculator::calculateAllValues).ifPresent(persistenceService::store);
                            latestMAC = null; // "reset" the mac to null to avoid misleading MAC addresses when an error happens *after* successfully reading a full packet
                            healthy = true;
                        }
                    }
                } catch (Exception ex) {
                    if (latestMAC != null) {
                        LOG.warn("Uncaught exception while handling measurements from MAC address \"" + latestMAC + "\", if this repeats and this is not a Ruuvitag, try blacklisting it", ex);
                    } else {
                        LOG.warn("Uncaught exception while handling measurements, this is an unexpected event. Please report this to https://github.com/Scrin/RuuviCollector/issues and include this log", ex);
                    }
                    LOG.debug("Offending line: " + line);
                }
            }
        } catch (IOException ex) {
            LOG.error("Uncaught exception while reading measurements", ex);
            return false;
        }
        return healthy;
    }
}
