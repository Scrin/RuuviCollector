package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.db.DummyDBConnection;
import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import fi.tkgwf.ruuvi.db.LegacyInfluxDBConnection;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DiscardUntilEnoughTimeHasElapsedStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public abstract class Config {

    private static final Logger LOG = Logger.getLogger(Config.class);
    private static final String RUUVI_COLLECTOR_PROPERTIES = "ruuvi-collector.properties";
    private static final String RUUVI_NAMES_PROPERTIES = "ruuvi-names.properties";

    private static final String DEFAULT_SCAN_COMMAND = "hcitool lescan --duplicates --passive";
    private static final String DEFAULT_DUMP_COMMAND = "hcidump --raw";

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
    private static String[] scanCommand = DEFAULT_SCAN_COMMAND.split(" ");
    private static String[] dumpCommand = DEFAULT_DUMP_COMMAND.split(" ");
    private static DBConnection dbConnection = null;
    private static Supplier<Long> timestampProvider = System::currentTimeMillis;
    private static LimitingStrategy limitingStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();
    private static Double defaultWithMotionSensitivityStrategyThreshold = 0.05;
    private static int defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep = 3;
    private static Map<String, TagProperties> tagProperties = new HashMap<>();

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
                influxUrl = props.getProperty("influxUrl", influxUrl);
                influxDatabase = props.getProperty("influxDatabase", influxDatabase);
                influxMeasurement = props.getProperty("influxMeasurement", influxMeasurement);
                influxUser = props.getProperty("influxUser", influxUser);
                influxPassword = props.getProperty("influxPassword", influxPassword);
                measurementUpdateLimit = parseLong(props, "measurementUpdateLimit", measurementUpdateLimit);
                storageMethod = props.getProperty("storage.method", storageMethod);
                storageValues = props.getProperty("storage.values", storageValues);
                filterMode = parseFilterMode(props);
                FILTER_MACS.addAll(parseFilterMacs(props));
                scanCommand = props.getProperty("command.scan", DEFAULT_SCAN_COMMAND).split(" ");
                dumpCommand = props.getProperty("command.dump", DEFAULT_DUMP_COMMAND).split(" ");
                influxRetentionPolicy = props.getProperty("influxRetentionPolicy", influxRetentionPolicy);
                influxGzip = parseBoolean(props, "influxGzip", influxGzip);
                influxBatch = parseBoolean(props, "influxBatch", influxBatch);
                influxBatchMaxSize = parseInteger(props, "influxBatchMaxSize", influxBatchMaxSize);
                influxBatchMaxTimeMs = parseInteger(props, "influxBatchMaxTime", influxBatchMaxTimeMs);
                limitingStrategy = parseLimitingStrategy(props);
                defaultWithMotionSensitivityStrategyThreshold = parseDouble(props, "limitingStrategy.defaultWithMotionSensitivity.threshold", defaultWithMotionSensitivityStrategyThreshold);
                defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep = parseInteger(props, "limitingStrategy.defaultWithMotionSensitivity.numberOfMeasurementsToKeep", defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep);
                tagProperties = parseTagProperties(props);

            }
        } catch (URISyntaxException | IOException ex) {
            LOG.warn("Failed to read configuration, using default values...", ex);
        }
    }

    private static Map<String, TagProperties> parseTagProperties(final Properties props) {
        final Map<String, Map<String, String>> tagProps = props.entrySet().stream()
            .map(e -> Pair.of(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
            .filter(p -> p.getLeft().startsWith("tag."))
            .collect(Collectors.groupingBy(extractMacAddressFromTagPropertyName(),
                toMap(extractKeyFromTagPropertyName(), Pair::getRight)));
        return tagProps.entrySet().stream().map(e -> {
            final TagProperties.Builder builder = TagProperties.builder(e.getKey());
            e.getValue().forEach(builder::add);
            return builder.build();
        }).collect(Collectors.toMap(TagProperties::getMac, t -> t));
    }

    private static Function<Pair<String, String>, String> extractKeyFromTagPropertyName() {
        return p -> p.getLeft().substring(17, p.getLeft().length());
    }

    private static Function<Pair<String, String>, String> extractMacAddressFromTagPropertyName() {
        return p -> p.getLeft().substring(4, 16);
    }

    private static LimitingStrategy parseLimitingStrategy(final Properties props) {
        final String strategy = props.getProperty("limitingStrategy");
        if (strategy != null) {
            if ("defaultWithMotionSensitivity".equals(strategy)) {
                return new DefaultDiscardingWithMotionSensitivityStrategy();
            }
        }
        return new DiscardUntilEnoughTimeHasElapsedStrategy();
    }

    private static Collection<? extends String> parseFilterMacs(final Properties props) {
        return Optional.ofNullable(props.getProperty("filter.macs"))
            .map(value -> Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> s.length() == 12)
                .map(String::toUpperCase).collect(toSet()))
            .orElse(Collections.emptySet());
    }

    private static Predicate<String> parseFilterMode(final Properties props) {
        final String filter = props.getProperty("filter.mode");
        if (filter != null) {
            switch (filter) {
                case "blacklist":
                    return (s) -> !FILTER_MACS.contains(s);
                case "whitelist":
                    return FILTER_MACS::contains;
            }
        }
        return filterMode;
    }

    private static long parseLong(final Properties props, final String key, final long defaultValue) {
        return parseNumber(props, key, defaultValue, Long::parseLong);
    }

    private static int parseInteger(final Properties props, final String key, final int defaultValue) {
        return parseNumber(props, key, defaultValue, Integer::parseInt);
    }

    private static double parseDouble(final Properties props, final String key, final double defaultValue) {
        return parseNumber(props, key, defaultValue, Double::parseDouble);
    }

    private static <N extends Number> N parseNumber(final Properties props, final String key, final N defaultValue, final Function<String, N> parser) {
        final String value = props.getProperty(key);
        try {
            return Optional.ofNullable(value).map(parser).orElse(defaultValue);
        } catch (final NumberFormatException ex) {
            LOG.warn("Malformed number format for " + key + ": '" + value + '\'');
            return defaultValue;
        }
    }

    private static boolean parseBoolean(final Properties props, final String key, final boolean defaultValue) {
        return Optional.ofNullable(props.getProperty(key)).map(Boolean::parseBoolean).orElse(defaultValue);
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
        if (dbConnection == null) {
            dbConnection = createDBConnection();
        }
        return dbConnection;
    }

    private static DBConnection createDBConnection() {
        switch (storageMethod) {
            case "influxdb":
                return new InfluxDBConnection();
            case "influxdb_legacy":
                return new LegacyInfluxDBConnection();
            case "dummy":
                return new DummyDBConnection();
            default:
                try {
                    LOG.info("Trying to use custom DB dbConnection class: " + storageMethod);
                    return (DBConnection) Class.forName(storageMethod).newInstance();
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Invalid storage method: " + storageMethod, e);
                }
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

    public static Supplier<Long> getTimestampProvider() {
        return timestampProvider;
    }

    public static LimitingStrategy getLimitingStrategy() {
        return limitingStrategy;
    }

    public static LimitingStrategy getLimitingStrategy(String mac) {
        return tagProperties.getOrDefault(mac, TagProperties.defaultValues()).getLimitingStrategy();
    }

    public static Double getDefaultWithMotionSensitivityStrategyThreshold() {
        return defaultWithMotionSensitivityStrategyThreshold;
    }

    public static int getDefaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep() {
        return defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep;
    }
}
