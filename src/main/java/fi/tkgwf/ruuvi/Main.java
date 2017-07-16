package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.db.DummyDBConnection;
import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV2;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV4;
import fi.tkgwf.ruuvi.utils.RuuviUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private final List<BeaconHandler> beaconHandlers = new LinkedList<>();

    public void initializeHandlers() {
        beaconHandlers.add(new DataFormatV2());
        beaconHandlers.add(new DataFormatV3());
        beaconHandlers.add(new DataFormatV4());
    }

    public static void main(String[] args) {

        // Start the collector..
        Main m = new Main();
        m.initializeHandlers();
        if (!m.run()) {
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

    private boolean run() {
        DBConnection db = Config.isDryrunMode() ? new DummyDBConnection() : new InfluxDBConnection();
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
            String line, latestMAC = null;
            while ((line = reader.readLine()) != null) {
                if (!dataReceived) {
                    LOG.info("Successfully reading data from hcidump");
                    dataReceived = true;
                }
                try {
                    if (line.startsWith("> ")) {
                        latestMAC = RuuviUtils.getMacFromLine(line.substring(23));
                    }
                    String finalLine = line, finalMAC = latestMAC;// lambdas require these to be effectively final
                    beaconHandlers.stream()
                            .map(handler -> handler.read(finalLine, finalMAC))
                            .filter(measurement -> measurement != null)
                            .forEach(db::post);
                } catch (Exception ex) {
                    LOG.warn("Uncaught exception while handling measurements from MAC address \"" + latestMAC + "\", if this repeats and this is not a Ruuvitag, consider blacklisting it", ex);
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
