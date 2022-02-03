package fi.tkgwf.ruuvi.service;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.config.ConfigTest;
import fi.tkgwf.ruuvi.db.DBConnection;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.utils.HCIParser;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceServiceTest {

    @BeforeEach
    void resetConfigBefore() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @AfterAll
    static void resetConfigAfter() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @Test
    void testApplyingCustomLimitingStrategy() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final BeaconHandler handler = new BeaconHandler();
        final MockConnection mockConnection = new MockConnection();
        final LimitingStrategy testingStrategy = new TestingStrategy();
        final PersistenceService service = new PersistenceService(mockConnection, testingStrategy);

        service.store(withRssi(handler.handle(hciData).get(), 1));
        service.store(withRssi(handler.handle(hciData).get(), 2));
        service.store(withRssi(handler.handle(hciData).get(), 3));
        service.store(withRssi(handler.handle(hciData).get(), 4));
        service.store(withRssi(handler.handle(hciData).get(), 5));
        service.store(withRssi(handler.handle(hciData).get(), 6));
        service.store(withRssi(handler.handle(hciData).get(), 7));
        service.store(withRssi(handler.handle(hciData).get(), 8));
        service.store(withRssi(handler.handle(hciData).get(), 9));
        service.store(withRssi(handler.handle(hciData).get(), 10));
        service.store(withRssi(handler.handle(hciData).get(), 11));

        final List<EnhancedRuuviMeasurement> results = mockConnection.getMeasurements();
        assertEquals(5, results.size());
        assertEquals(1, results.get(0).getRssi().intValue());
        assertEquals(5, results.get(1).getRssi().intValue());
        assertEquals(8, results.get(2).getRssi().intValue());
        assertEquals(10, results.get(3).getRssi().intValue());
        assertEquals(11, results.get(4).getRssi().intValue());
    }

    @Test
    void testApplyingDifferentLimitingStrategiesToDifferentDevices() {
        final Properties properties = new Properties();
        properties.put("filter.mode", "whitelist");
        properties.put("tag.F1E2D3C4B5A6.limitingStrategy", "onMovement");
        Config.readConfigFromProperties(properties);

        final MockConnection mockConnection = new MockConnection();
        final LimitingStrategy testingStrategy = new TestingStrategy();
        final PersistenceService service = new PersistenceService(mockConnection, testingStrategy);

        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData.mac = "ABCDEF012345";
        final HCIData hciData2 = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData2.mac = "F1E2D3C4B5A6";

        setClockToMilliseconds(0);
        final BeaconHandler handler = new BeaconHandler();

        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 1), 1d)); // Store: first instance
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 2), 1d)); // Not: no time passed
        setClockToMilliseconds(1000);
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 3), 1d)); // Not: still not enough time passed
        setClockToMilliseconds(3000);
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 4), 1d)); // Not: same as above
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 5), 1d)); // Store: first instance
        setClockToMilliseconds(6000);
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 6), 1.99d)); // Not: still not enough time passed; not using movement detection
        setClockToMilliseconds(9000);
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 7), 1.99d)); // Store: movement detected
        setClockToMilliseconds(10000);
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 8), 1d)); // Store: enough time has passed
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 9), 1d)); // Store: movement detected
        setClockToMilliseconds(11000);
        service.store(withAcceleration(withRssi(handler.handle(hciData).get(), 10), 1d)); // Not: not enough time passed
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 11), 1d)); // Store: happens right after movement was detected
        setClockToMilliseconds(12000);
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 12), 1d)); // Not: no movement and not enough time passed
        setClockToMilliseconds(90000);
        service.store(withAcceleration(withRssi(handler.handle(hciData2).get(), 13), 1d)); // Store: enough time passed

        final List<EnhancedRuuviMeasurement> results = mockConnection.getMeasurements();
//        assertEquals(8, results.size());
        assertEquals(1, results.get(0).getRssi().intValue());
        assertEquals(5, results.get(1).getRssi().intValue());
        assertEquals(7, results.get(2).getRssi().intValue());
        assertEquals(8, results.get(3).getRssi().intValue());
        assertEquals(9, results.get(4).getRssi().intValue());
        assertEquals(10, results.get(5).getRssi().intValue());
        assertEquals(11, results.get(6).getRssi().intValue());
        assertEquals(13, results.get(7).getRssi().intValue());
    }

    private EnhancedRuuviMeasurement withAcceleration(final EnhancedRuuviMeasurement measurement, double accelerationX) {
        measurement.setAccelerationX(accelerationX);
        return measurement;
    }

    private void setClockToMilliseconds(final long millis) {
        TestFixture.setClockToMilliseconds(() -> millis);
    }

    private static EnhancedRuuviMeasurement withRssi(final EnhancedRuuviMeasurement measurement, final int rssi) {
        measurement.setRssi(rssi);
        return measurement;
    }

    public static class MockConnection implements DBConnection {

        private final ArrayList<EnhancedRuuviMeasurement> measurements = new ArrayList<>();
        private boolean closeCalled = false;

        @Override
        public void save(final EnhancedRuuviMeasurement measurement) {
            this.measurements.add(measurement);
        }

        @Override
        public void close() {
            this.closeCalled = true;
        }

        List<EnhancedRuuviMeasurement> getMeasurements() {
            return measurements;
        }

        boolean isCloseCalled() {
            return closeCalled;
        }
    }

    public static class TestingStrategy implements LimitingStrategy {

        @Override
        public Optional<EnhancedRuuviMeasurement> apply(final EnhancedRuuviMeasurement measurement) {
            if (Arrays.asList(1, 5, 8, 10, 11).contains(measurement.getRssi())) {
                return Optional.of(measurement);
            }
            return Optional.empty();
        }
    }
}
