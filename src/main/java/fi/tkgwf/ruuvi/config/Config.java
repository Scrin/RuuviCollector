package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.db.DummyDBConnection;
import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import fi.tkgwf.ruuvi.db.LegacyInfluxDBConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public abstract class Config {

    private static final Logger LOG = Logger.getLogger(Config.class);
    private static final String RUUVI_COLLECTOR_PROPERTIES = "ruuvi-collector.properties";
    private static final String RUUVI_NAMES_PROPERTIES = "ruuvi-names.properties";

    private static String influxUrl = "http://localhost:8086";
    private static String influxDatabase = "ruuvi";
    private static String influxMeasurement = "ruuvi_measurements";
    private static String influxUser = "ruuvi";
    private static String influxPassword = "ruuvi";
    private static String influxRetentionPolicy = "autogen";
    private static boolean influxGzip = true;
    private static boolean influxBatch = true;
    private static int influxBatchMaxSize = 2000;
    private static int influxBatchMaxTimeMs = 100;
    private static long measurementUpdateLimit = 9900;
    private static String storageMethod = "influxdb";
    private static String storageValues = "extended";
    private static Predicate<String> filterMode = (s) -> true;
    private static final Set<String> FILTER_MACS = new HashSet<>();
    private static final Map<String, String> TAG_NAMES = new HashMap<>();
    private static String[] scanCommand = {"hcitool", "lescan", "--duplicates", "--passive"};
    private static String[] dumpCommand = {"hcidump", "--raw"};
    private static Clock clock = Clock.systemUTC();

    static {
        readConfig();
        readTagNames();
    }

    private static void readConfig() {
        try {
            final File configFile = findConfigFiles(RUUVI_COLLECTOR_PROPERTIES);
            if (configFile != null) {
                LOG.debug("Config: " + configFile);
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                Enumeration<?> e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = props.getProperty(key);
                    switch (key) {
                        case "influxUrl":
                            influxUrl = value;
                            break;
                        case "influxDatabase":
                            influxDatabase = value;
                            break;
                        case "influxMeasurement":
                            influxMeasurement = value;
                            break;
                        case "influxUser":
                            influxUser = value;
                            break;
                        case "influxPassword":
                            influxPassword = value;
                            break;
                        case "measurementUpdateLimit":
                            try {
                                measurementUpdateLimit = Long.parseLong(value);
                            } catch (NumberFormatException ex) {
                                LOG.warn("Malformed number format for influxUpdateLimit: '" + value + '\'');
                            }
                            break;
                        case "storage.method":
                            storageMethod = value;
                            break;
                        case "storage.values":
                            storageValues = value;
                            break;
                        case "filter.mode":
                            switch (value) {
                                case "blacklist":
                                    filterMode = (s) -> !FILTER_MACS.contains(s);
                                    break;
                                case "whitelist":
                                    filterMode = FILTER_MACS::contains;
                            }
                            break;
                        case "filter.macs":
                            Arrays.stream(value.split(","))
                                    .map(String::trim)
                                    .filter(s -> s.length() == 12)
                                    .map(String::toUpperCase)
                                    .forEach(FILTER_MACS::add);
                            break;
                        case "command.scan":
                            scanCommand = value.split(" ");
                            break;
                        case "command.dump":
                            dumpCommand = value.split(" ");
                            break;
                        case "influxRetentionPolicy":
                            influxRetentionPolicy = value;
                            break;
                        case "influxGzip":
                            influxGzip = Boolean.parseBoolean(value);
                            break;
                        case "influxBatch":
                            influxBatch = Boolean.parseBoolean(value);
                            break;
                        case "influxBatchMaxSize":
                            try {
                                influxBatchMaxSize = Integer.parseInt(value);
                            } catch (NumberFormatException ex) {
                                LOG.warn("Malformed number format for influxBatchMaxSize: '" + value + '\'');
                            }
                            break;
                        case "influxBatchMaxTime":
                            try {
                                influxBatchMaxTimeMs = Integer.parseInt(value);
                            } catch (NumberFormatException ex) {
                                LOG.warn("Malformed number format for influxBatchMaxTime: '" + value + '\'');
                            }
                            break;
                    }
                }
            }
        } catch (URISyntaxException | IOException ex) {
            LOG.warn("Failed to read configuration, using default values...", ex);
        }
    }

    private static File findConfigFiles(final String propertiesFileName) throws URISyntaxException {
        File jarLocation = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        File[] configFiles = jarLocation.listFiles(f -> f.isFile() && f.getName().equals(propertiesFileName));
        if (configFiles == null || configFiles.length == 0) {
            // look for config files in the parent directory if none found in the current directory, this is useful during development when
            // RuuviCollector can be run from maven target directory directly while the config file sits in the project root
            configFiles = jarLocation.getParentFile().listFiles(f -> f.isFile() && f.getName().equals(propertiesFileName));
            if (configFiles == null || configFiles.length == 0) {
                // Finally, let the class loader try to look for the config file resource:
                configFiles = Optional.ofNullable(Config.class.getResource(String.format("/%s", propertiesFileName)))
                        .map(url -> {
                            try {
                                return url.toURI();
                            } catch (final URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .map(File::new)
                        .map(f -> new File[]{f})
                        .orElse(null);
            }
        }
        return Optional.ofNullable(configFiles).map(f -> f[0]).orElse(null);
    }

    private static void readTagNames() {
        try {
            final File configFile = findConfigFiles(RUUVI_NAMES_PROPERTIES);
            if (configFile != null) {
                LOG.debug("Tag names: " + configFile);
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                Enumeration<?> e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String key = StringUtils.trimToEmpty((String) e.nextElement()).toUpperCase();
                    String value = StringUtils.trimToEmpty(props.getProperty(key));
                    if (key.length() == 12 && value.length() > 0) {
                        TAG_NAMES.put(key, value);
                    }
                }
            }
        } catch (URISyntaxException | IOException ex) {
            LOG.warn("Failed to read tag names", ex);
        }
    }

    public static DBConnection getDBConnection() {
        switch (storageMethod) {
            case "influxdb":
                return new InfluxDBConnection();
            case "influxdb_legacy":
                return new LegacyInfluxDBConnection();
            case "dummy":
                return new DummyDBConnection();
            default:
                throw new IllegalArgumentException("Invalid storage method: " + storageMethod);
        }
    }

    public static String getStorageValues() {
        return storageValues;
    }

    public static String getInfluxUrl() {
        return influxUrl;
    }

    public static String getInfluxDatabase() {
        return influxDatabase;
    }

    public static String getInfluxMeasurement() {
        return influxMeasurement;
    }

    public static String getInfluxUser() {
        return influxUser;
    }

    public static String getInfluxPassword() {
        return influxPassword;
    }

    public static String getInfluxRetentionPolicy() {
        return influxRetentionPolicy;
    }

    public static boolean isInfluxGzip() {
        return influxGzip;
    }

    public static boolean isInfluxBatch() {
        return influxBatch;
    }

    public static int getInfluxBatchMaxSize() {
        return influxBatchMaxSize;
    }

    public static int getInfluxBatchMaxTimeMs() {
        return influxBatchMaxTimeMs;
    }

    public static long getMeasurementUpdateLimit() {
        return measurementUpdateLimit;
    }

    public static boolean isAllowedMAC(String mac) {
        return filterMode.test(mac);
    }

    public static String[] getScanCommand() {
        return scanCommand;
    }

    public static String[] getDumpCommand() {
        return dumpCommand;
    }

    public static String getTagName(String mac) {
        return TAG_NAMES.get(mac);
    }

    public static long currentTimeMillis() {
        return clock.millis();
    }
}
