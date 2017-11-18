package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.db.InfluxDBConnection;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;

/**
 * Terrible class for migrating a Terrible format to a more sensible one. I am
 * sorry for what you'll find below.
 */
public class InfluxDataMigrator {

    private static final Logger LOG = Logger.getLogger(InfluxDataMigrator.class);
    private static final int QUEUE_SIZE = 4000;
    private static final int QUERY_SIZE = 2000;
    private static final int BATCH_SIZE = 2000;
    private static final long TIME_ERROR_TOLERANCE_MS = 80;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final LegacyMeasurement EMPTY_MEASUREMENT = new LegacyMeasurement();

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, BlockingQueue<LegacyMeasurement>> queues = new HashMap<>();
    private final Map<String, AtomicBoolean> doneQueues = new HashMap<>();
    private final Map<String, List<LegacyMeasurement>> pendingMeasurements = new HashMap<>();
    private final Map<String, AtomicLong> discardedMeasurements = new HashMap<>();

    public synchronized void migrate() {
        LOG.info("Starting migration...");
        long start = System.currentTimeMillis();
        DBConnection db = new InfluxDBConnection();
        // Theres a hard limit (?) of 5 concurrent queries per instance
        InfluxDB influx1 = createInfluxDB();
        InfluxDB influx2 = createInfluxDB();
        BlockingQueue<LegacyMeasurement> temperatureQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> humidityQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> pressureQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> accelerationXQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> accelerationYQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> accelerationZQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> batteryQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        BlockingQueue<LegacyMeasurement> rssiQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        Query temperatureQuery = new Query("select * from temperature order by time asc", Config.getInfluxDatabase());
        Query humidityQuery = new Query("select * from humidity order by time asc", Config.getInfluxDatabase());
        Query pressureQuery = new Query("select * from pressure order by time asc", Config.getInfluxDatabase());
        Query accelerationXQuery = new Query("select * from acceleration where \"axis\"='x' order by time asc", Config.getInfluxDatabase());
        Query accelerationYQuery = new Query("select * from acceleration where \"axis\"='y' order by time asc", Config.getInfluxDatabase());
        Query accelerationZQuery = new Query("select * from acceleration where \"axis\"='z' order by time asc", Config.getInfluxDatabase());
        Query batteryQuery = new Query("select * from batteryVoltage order by time asc", Config.getInfluxDatabase());
        Query rssiQuery = new Query("select * from rssi order by time asc", Config.getInfluxDatabase());
        AtomicBoolean temperatureDone = new AtomicBoolean(false);
        AtomicBoolean humidityDone = new AtomicBoolean(false);
        AtomicBoolean pressureDone = new AtomicBoolean(false);
        AtomicBoolean accelerationXDone = new AtomicBoolean(false);
        AtomicBoolean accelerationYDone = new AtomicBoolean(false);
        AtomicBoolean accelerationZDone = new AtomicBoolean(false);
        AtomicBoolean batteryDone = new AtomicBoolean(false);
        AtomicBoolean rssiDone = new AtomicBoolean(false);
        queues.put("temperature", temperatureQueue);
        queues.put("humidity", humidityQueue);
        queues.put("pressure", pressureQueue);
        queues.put("accelerationX", accelerationXQueue);
        queues.put("accelerationY", accelerationYQueue);
        queues.put("accelerationZ", accelerationZQueue);
        queues.put("battery", batteryQueue);
        queues.put("rssi", rssiQueue);
        doneQueues.put("temperature", temperatureDone);
        doneQueues.put("humidity", humidityDone);
        doneQueues.put("pressure", pressureDone);
        doneQueues.put("accelerationX", accelerationXDone);
        doneQueues.put("accelerationY", accelerationYDone);
        doneQueues.put("accelerationZ", accelerationZDone);
        doneQueues.put("battery", batteryDone);
        doneQueues.put("rssi", rssiDone);
        queues.keySet().forEach(k -> {
            pendingMeasurements.put(k, new LinkedList<>());
            discardedMeasurements.put(k, new AtomicLong());
        });
        LOG.info("Starting query threads...");
        influx1.query(temperatureQuery, QUERY_SIZE, new MigrationConsumer(temperatureQueue, temperatureDone));
        influx1.query(humidityQuery, QUERY_SIZE, new MigrationConsumer(humidityQueue, humidityDone));
        influx1.query(pressureQuery, QUERY_SIZE, new MigrationConsumer(pressureQueue, pressureDone));
        influx1.query(batteryQuery, QUERY_SIZE, new MigrationConsumer(batteryQueue, batteryDone));
        influx2.query(rssiQuery, QUERY_SIZE, new MigrationConsumer(rssiQueue, rssiDone));
        influx2.query(accelerationXQuery, QUERY_SIZE, new MigrationConsumer(accelerationXQueue, accelerationXDone));
        influx2.query(accelerationYQuery, QUERY_SIZE, new MigrationConsumer(accelerationYQueue, accelerationYDone));
        influx2.query(accelerationZQuery, QUERY_SIZE, new MigrationConsumer(accelerationZQueue, accelerationZDone));
        LOG.info("Processing...");
        long counter = 0;
        try {
            while (!done()) {
                while (pendingQueues()) {
                    Thread.sleep(50);
                }
                /*
                if (System.currentTimeMillis() % 20 == 0) { // reduce "debug" spam by logging only about 5% of the times
                    queues.forEach((k, v) -> System.out.println(k + ": " + v.size() + " + " + pendingMeasurements.get(k).size() + " - " + discardedMeasurements.get(k).get()));
                    long duration = System.currentTimeMillis() - start;
                    System.out.println("progress: " + counter + " elapsed: " + duration / 1000d + " sec " + (counter / (duration / 1000d)) + " per sec");
                    System.out.println("-----");
                }
                 */
                LegacyMeasurement temperature = temperatureQueue.poll();
                if (temperature == null) {
                    break;
                } else if (temperature.mac == null) {
                    continue;
                }
                String currentMac = temperature.mac;
                long currentTime = temperature.time;

                LegacyMeasurement humidity = find("humidity", currentMac, currentTime);
                LegacyMeasurement pressure = find("pressure", currentMac, currentTime);
                LegacyMeasurement accelerationX = find("accelerationX", currentMac, currentTime);
                LegacyMeasurement accelerationY = find("accelerationY", currentMac, currentTime);
                LegacyMeasurement accelerationZ = find("accelerationZ", currentMac, currentTime);
                LegacyMeasurement battery = find("battery", currentMac, currentTime);
                LegacyMeasurement rssi = find("rssi", currentMac, currentTime);

                RuuviMeasurement m = new RuuviMeasurement();
                m.time = temperature.time;
                m.mac = currentMac;
                m.dataFormat = temperature.dataFormat != null ? Integer.valueOf(temperature.dataFormat) : null;
                m.temperature = temperature.value;
                m.humidity = humidity.value;
                m.pressure = pressure.value;
                if (accelerationX.value != null && accelerationY.value != null && accelerationZ.value != null) {
                    m.accelerationX = accelerationX.value;
                    m.accelerationY = accelerationY.value;
                    m.accelerationZ = accelerationZ.value;
                }
                m.batteryVoltage = battery.value;
                m.rssi = rssi.value != null ? rssi.value.intValue() : null;

                MeasurementValueCalculator.calculateAllValues(m);
                db.save(m);
                counter++;
            }
        } catch (InterruptedException ex) {
            LOG.error("Interrupted", ex);
        }
        long duration = System.currentTimeMillis() - start;
        LOG.info("Finished migration! " + counter + " measurements migrated, took " + duration / 1000d + " seconds (" + (counter / (duration / 1000d)) + " measurements per second)");
        queues.keySet().stream().sorted().forEach(k -> LOG.info(k + " discarded: " + (pendingMeasurements.get(k).size() + discardedMeasurements.get(k).get())));
        influx1.close();
        influx2.close();
        db.close();
    }

    private LegacyMeasurement find(String queueName, String mac, long time) {
        Predicate<LegacyMeasurement> isValidTime = m -> time > m.time - TIME_ERROR_TOLERANCE_MS && time < m.time + TIME_ERROR_TOLERANCE_MS;
        Predicate<LegacyMeasurement> isValidMac = m -> mac.equals(m.mac);
        List<LegacyMeasurement> pending = pendingMeasurements.get(queueName);
        // Remove old (orphaned) measurements for this MAC
        for (Iterator<LegacyMeasurement> i = pending.iterator(); i.hasNext();) {
            LegacyMeasurement measurement = i.next();
            if (isValidMac.test(measurement) && measurement.time + TIME_ERROR_TOLERANCE_MS < time) {
                i.remove();
                discardedMeasurements.get(queueName).incrementAndGet();
            }
        }
        // Check if the pending list has a valid entry
        LegacyMeasurement m = pending.stream().filter(isValidMac).findAny().orElse(null);
        if (m != null) {
            if (isValidTime.test(m)) {
                pending.remove(m);
                return m;
            }
        }

        LegacyMeasurement peek = queues.get(queueName).peek();
        if (peek != null && peek.time - TIME_ERROR_TOLERANCE_MS > time) {
            return EMPTY_MEASUREMENT; // The queue has way too new instances
        }

        while (true) {
            m = queues.get(queueName).poll();
            if (m == null) {
                return EMPTY_MEASUREMENT;
            } else if (m.mac == null) {
                // discard all values without MAC
            } else if (mac.equals(m.mac)) {
                if (isValidTime.test(m)) {
                    return m;
                } else {
                    pending.add(m);
                    return EMPTY_MEASUREMENT;
                }
            } else {
                pending.add(m);
                if (!isValidTime.test(m)) {
                    return EMPTY_MEASUREMENT;
                }
            }
        }
    }

    private InfluxDB createInfluxDB() {
        InfluxDB influxDB = InfluxDBFactory.connect(Config.getInfluxUrl(), Config.getInfluxUser(), Config.getInfluxPassword());
        influxDB.setDatabase(Config.getInfluxDatabase());
        influxDB.enableGzip();
        influxDB.enableBatch(BATCH_SIZE, 100, TimeUnit.MILLISECONDS);
        return influxDB;
    }

    private boolean pendingQueues() {
        lock.lock();
        try {
            return queues.entrySet().stream().filter(e -> e.getValue().isEmpty() && !doneQueues.get(e.getKey()).get()).findAny().isPresent();
        } finally {
            lock.unlock();
        }
    }

    private boolean done() {
        lock.lock();
        try {
            return doneQueues.values().stream().filter(AtomicBoolean::get).count() >= queues.size()
                    && queues.entrySet().stream().filter(e -> e.getValue().isEmpty()).count() >= queues.size();
        } finally {
            lock.unlock();
        }
    }

    private class MigrationConsumer implements Consumer<QueryResult> {

        private final BlockingQueue<LegacyMeasurement> q;
        private final AtomicBoolean done;

        public MigrationConsumer(BlockingQueue<LegacyMeasurement> q, AtomicBoolean done) {
            this.q = q;
            this.done = done;
        }

        @Override
        public void accept(QueryResult r) {
            String error = r.getError();
            if (StringUtils.isNotBlank(error)) {
                if (error.equals("DONE")) {
                    lock.lock();
                    try {
                        done.set(true);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    LOG.error("Influx returned error: " + error);
                }
                return;
            }
            r.getResults().stream().map(Result::getSeries).filter(Objects::nonNull).flatMap(List::stream).forEach(s -> {
                List<String> columns = s.getColumns();
                int timeIndex = -1;
                int macIndex = -1;
                int sourceIndex = -1;
                int dataFormatIndex = -1;
                int valueIndex = -1;
                int axisIndex = -1;
                for (int i = 0; i < columns.size(); i++) {
                    switch (columns.get(i)) {
                        case "time":
                            timeIndex = i;
                            break;
                        case "mac":
                            macIndex = i;
                            break;
                        case "source":
                            sourceIndex = i;
                            break;
                        case "dataFormatIndex":
                            dataFormatIndex = i;
                            break;
                        case "value":
                            valueIndex = i;
                            break;
                        case "axis":
                            axisIndex = i;
                            break;
                    }
                }
                for (List v : s.getValues()) {
                    LegacyMeasurement m = new LegacyMeasurement();
                    String timeString = safeGet(v, timeIndex);
                    if (timeString != null) {
                        m.time = Instant.from(FORMATTER.parse(timeString)).toEpochMilli();
                    }
                    m.mac = safeGet(v, macIndex);
                    if (m.mac == null) {
                        m.mac = safeGet(v, sourceIndex);
                    }
                    m.dataFormat = safeGet(v, dataFormatIndex);
                    m.value = Double.valueOf(safeGet(v, valueIndex));
                    try {
                        q.put(m);
                    } catch (InterruptedException ex) {
                        LOG.error("Interrupted", ex);
                    }
                }
            });
        }

        private String safeGet(List l, int index) {
            if (index == -1) {
                return null;
            }
            Object o = l.get(index);
            return o == null ? null : o.toString();
        }
    }

    private static class LegacyMeasurement {

        Long time;
        String mac;
        String dataFormat;
        Double value;
    }
}
