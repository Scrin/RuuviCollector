package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.db.DummyDBConnection;
import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import fi.tkgwf.ruuvi.db.LegacyInfluxDBConnection;
import fi.tkgwf.ruuvi.db.PrometheusExporter;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DiscardUntilEnoughTimeHasElapsedStrategy;
import fi.tkgwf.ruuvi.utils.InfluxDBConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
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

    private static String influxUrl;
    private static String influxDatabase;
    private static String influxMeasurement;
    private static String influxUser;
    private static String influxPassword;
    private static String influxRetentionPolicy;
    private static boolean influxGzip;
    private static boolean influxBatch;
    private static boolean exitOnInfluxDBIOException;
    private static int influxBatchMaxSize;
    private static int influxBatchMaxTimeMs;
    private static long measurementUpdateLimit;
    private static String storageMethod;
    private static String storageValues;
    private static final Set<String> FILTER_INFLUXDB_FIELDS = new HashSet<>();
    private static Predicate<String> influxDbFieldFilter;
    private static Predicate<String> filterMode;
    private static final Set<String> FILTER_MACS = new HashSet<>();
    private static final Map<String, String> TAG_NAMES = new HashMap<>();
    private static String receiver = ""; // used to tag received values with an identifier associated to this service
    private static String[] scanCommand;
    private static String[] dumpCommand;
    private static DBConnection dbConnection;
    private static Supplier<Long> timestampProvider;
    private static LimitingStrategy limitingStrategy;
    private static Double defaultWithMotionSensitivityStrategyThreshold;
    private static int defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep;
    private static Map<String, TagProperties> tagProperties;
    private static Function<String, File> configFileFinder;
    private static int prometheusHttpPort;

    static {
        reload();
    }

    public static void reload() {
        reload(defaultConfigFileFinder());
    }

    public static void reload(final Function<String, File> configFileFinder) {
        Config.configFileFinder = configFileFinder;
        loadDefaults();
        readTagNames();
        readConfig();
    }

    private static void loadDefaults() {
        influxUrl = "http://localhost:8086";
        influxDatabase = "ruuvi";
        influxMeasurement = "ruuvi_measurements";
        influxUser = "ruuvi";
        influxPassword = "ruuvi";
        influxRetentionPolicy = "autogen";
        influxGzip = true;
        influxBatch = true;
        exitOnInfluxDBIOException = false;
        influxBatchMaxSize = 2000;
        influxBatchMaxTimeMs = 100;
        measurementUpdateLimit = 9900;
        storageMethod = "influxdb";
        storageValues = "extended";
        FILTER_INFLUXDB_FIELDS.clear();
        influxDbFieldFilter = (s) -> true;
        filterMode = (s) -> true;
        FILTER_MACS.clear();
        TAG_NAMES.clear();
        scanCommand = DEFAULT_SCAN_COMMAND.split(" ");
        dumpCommand = DEFAULT_DUMP_COMMAND.split(" ");
        dbConnection = null;
        timestampProvider = System::currentTimeMillis;
        limitingStrategy = new DiscardUntilEnoughTimeHasElapsedStrategy();
        defaultWithMotionSensitivityStrategyThreshold = 0.05;
        defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep = 3;
        tagProperties = new HashMap<>();
        prometheusHttpPort = 9155;
    }

    private static void readConfig() {
        try {
            final File configFile = configFileFinder.apply(RUUVI_COLLECTOR_PROPERTIES);
            if (configFile != null) {
                LOG.debug("Config: " + configFile);
                Properties props = new Properties();
                props.load(new InputStreamReader(new FileInputStream(configFile), Charset.forName("UTF-8")));
                readConfigFromProperties(props);
            }
        } catch (IOException ex) {
            LOG.warn("Failed to read configuration, using default values...", ex);
        }
    }

    public static void readConfigFromProperties(final Properties props) {
        influxUrl = props.getProperty("influxUrl", influxUrl);
        influxDatabase = props.getProperty("influxDatabase", influxDatabase);
        influxMeasurement = props.getProperty("influxMeasurement", influxMeasurement);
        influxUser = props.getProperty("influxUser", influxUser);
        influxPassword = props.getProperty("influxPassword", influxPassword);
        measurementUpdateLimit = parseLong(props, "measurementUpdateLimit", measurementUpdateLimit);
        storageMethod = props.getProperty("storage.method", storageMethod);
        storageValues = props.getProperty("storage.values", storageValues);
        FILTER_INFLUXDB_FIELDS.addAll(parseFilterInfluxDbFields(props));
        influxDbFieldFilter = createInfluxDbFieldFilter();
        filterMode = parseFilterMode(props);
        FILTER_MACS.addAll(parseFilterMacs(props));
        receiver = props.getProperty("receiver", "");
        scanCommand = props.getProperty("command.scan", DEFAULT_SCAN_COMMAND).split(" ");
        dumpCommand = props.getProperty("command.dump", DEFAULT_DUMP_COMMAND).split(" ");
        influxRetentionPolicy = props.getProperty("influxRetentionPolicy", influxRetentionPolicy);
        influxGzip = parseBoolean(props, "influxGzip", influxGzip);
        influxBatch = parseBoolean(props, "influxBatch", influxBatch);
        exitOnInfluxDBIOException = parseBoolean(props, "exitOnInfluxDBIOException", exitOnInfluxDBIOException);
        influxBatchMaxSize = parseInteger(props, "influxBatchMaxSize", influxBatchMaxSize);
        influxBatchMaxTimeMs = parseInteger(props, "influxBatchMaxTime", influxBatchMaxTimeMs);
        limitingStrategy = parseLimitingStrategy(props);
        defaultWithMotionSensitivityStrategyThreshold = parseDouble(props, "limitingStrategy.defaultWithMotionSensitivity.threshold", defaultWithMotionSensitivityStrategyThreshold);
        defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep = parseInteger(props, "limitingStrategy.defaultWithMotionSensitivity.numberOfMeasurementsToKeep", defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep);
        tagProperties = parseTagProperties(props);
        prometheusHttpPort = parseInteger(props, "prometheusHttpPort", prometheusHttpPort);
        validateConfig();
    }

    private static void validateConfig() {
        if (FILTER_INFLUXDB_FIELDS.isEmpty()) {
            switch (storageValues) {
                case "whitelist":
                    throw new IllegalStateException("You have selected no fields to be stored into the InfluxDB. " +
                        "Please set the storage.values.list property or select another storage.values option. " +
                        "See MEASUREMENTS.md for the available fields and ruuvi-collector.properties.example for " +
                        "the possible values of the storage.values property.");
                case "blacklist":
                    LOG.warn("You have set storage.values=blacklist but left storage.values.list empty. " +
                        "This is essentially the same as setting storage.values=extended. If this is intentional, " +
                        "you may ignore this message.");
                    break;
            }
        }
    }

    private static Predicate<String> createInfluxDbFieldFilter() {
        return createInfluxDbFieldFilter(storageValues, FILTER_INFLUXDB_FIELDS);
    }

    static Predicate<String> createInfluxDbFieldFilter(final String value, final Collection<String> list) {
        switch (Optional.ofNullable(value).orElse("extended")) {
            case "raw":
                return InfluxDBConverter.RAW_STORAGE_VALUES::contains;
            case "extended":
                return s -> true;
            case "whitelist":
                return list::contains;
            case "blacklist":
                return s -> !list.contains(s);
            default:
                LOG.warn("Unknown storage.values value: " + value);
                return s -> true;
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
        return p -> p.getLeft().substring(17);
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

    private static Collection<String> parseFilterInfluxDbFields(final Properties props) {
        return parseFilterInfluxDbFields(props.getProperty("storage.values.list"));
    }

    static Collection<String> parseFilterInfluxDbFields(final String list) {
        return Optional.ofNullable(list)
            .map(value -> Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(toSet()))
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
                case "named":
                    if (TAG_NAMES.isEmpty()) {
                        throw new IllegalStateException(
                        "You have set filter.mode=named but left ruuvi-names.properties empty. " +
                        "Please select a different filter.mode value or populate ruuvi-names.properties.");
                    }
                    return TAG_NAMES.keySet()::contains;
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

    private static Function<String, File> defaultConfigFileFinder() {
        return propertiesFileName -> {
            try {
                final File jarLocation = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
                Optional<File> configFile = findConfigFile(propertiesFileName, jarLocation);
                if (!configFile.isPresent()) {
                    // look for config files in the parent directory if none found in the current directory, this is useful during development when
                    // RuuviCollector can be run from maven target directory directly while the config file sits in the project root
                    final File parentFile = jarLocation.getParentFile();
                    configFile = findConfigFile(propertiesFileName, parentFile);
                }
                return configFile.orElse(null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Optional<File> findConfigFile(String propertiesFileName, File parentFile) {
        return Optional.ofNullable(parentFile.listFiles(f -> f.isFile() && f.getName().equals(propertiesFileName)))
            .filter(configFiles -> configFiles.length > 0)
            .map(configFiles -> configFiles[0]);
    }

    private static void readTagNames() {
        try {
            final File configFile = configFileFinder.apply(RUUVI_NAMES_PROPERTIES);
            if (configFile != null) {
                LOG.debug("Tag names: " + configFile);
                Properties props = new Properties();
                props.load(new InputStreamReader(new FileInputStream(configFile), Charset.forName("UTF-8")));
                Enumeration<?> e = props.propertyNames();
                while (e.hasMoreElements()) {
                    String key = StringUtils.trimToEmpty((String) e.nextElement()).toUpperCase();
                    String value = StringUtils.trimToEmpty(props.getProperty(key));
                    if (key.length() == 12 && value.length() > 0) {
                        TAG_NAMES.put(key, value);
                    }
                }
            }
        } catch (IOException ex) {
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
        LOG.info("Creating database connection for storageMethod: " + storageMethod);
        switch (storageMethod) {
            case "influxdb":
                return new InfluxDBConnection();
            case "influxdb_legacy":
                return new LegacyInfluxDBConnection();
            case "prometheus":
                return new PrometheusExporter(getPrometheusHttpPort());
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

    /**
     * Use {@link #getAllowedInfluxDbFieldsPredicate()} instead.
     *
     * @return The value of the storage.values configuration property.
     */
    @Deprecated
    public static String getStorageValues() {
        return storageValues;
    }

    public static Predicate<String> getAllowedInfluxDbFieldsPredicate() {
        return influxDbFieldFilter;
    }

    public static Predicate<String> getAllowedInfluxDbFieldsPredicate(String mac) {
        return Optional.ofNullable(tagProperties.get(mac))
            .map(TagProperties::getInfluxDbFieldFilter)
            .orElse(influxDbFieldFilter);
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

    public static boolean exitOnInfluxDBIOException() {
        return exitOnInfluxDBIOException;
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
        return mac != null && filterMode.test(mac);
    }

    public static String getReceiver() {
        return receiver;
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

    public static int getPrometheusHttpPort() {
        return prometheusHttpPort;
    }

    public static Supplier<Long> getTimestampProvider() {
        return timestampProvider;
    }

    public static LimitingStrategy getLimitingStrategy() {
        return limitingStrategy;
    }

    public static LimitingStrategy getLimitingStrategy(String mac) {
        return Optional.ofNullable(tagProperties.get(mac))
            .map(TagProperties::getLimitingStrategy)
            .orElse(null);
    }

    public static Double getDefaultWithMotionSensitivityStrategyThreshold() {
        return defaultWithMotionSensitivityStrategyThreshold;
    }

    public static int getDefaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep() {
        return defaultWithMotionSensitivityStrategyNumberOfPreviousMeasurementsToKeep;
    }
}
