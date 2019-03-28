package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.config.ConfigTest;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.utils.HCIParser;
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
        final DataFormatV3 v3 = new DataFormatV3();
        final MainTest.MockConnection mockConnection = new MainTest.MockConnection();
        final LimitingStrategy testingStrategy = new TestingStrategy();
        final PersistenceService service = new PersistenceService(mockConnection, testingStrategy);

        service.store(withRssi(v3.handle(hciData), 1));
        service.store(withRssi(v3.handle(hciData), 2));
        service.store(withRssi(v3.handle(hciData), 3));
        service.store(withRssi(v3.handle(hciData), 4));
        service.store(withRssi(v3.handle(hciData), 5));
        service.store(withRssi(v3.handle(hciData), 6));
        service.store(withRssi(v3.handle(hciData), 7));
        service.store(withRssi(v3.handle(hciData), 8));
        service.store(withRssi(v3.handle(hciData), 9));
        service.store(withRssi(v3.handle(hciData), 10));
        service.store(withRssi(v3.handle(hciData), 11));

        final List<RuuviMeasurement> results = mockConnection.getMeasurements();
        assertEquals(5, results.size());
        assertEquals(1, results.get(0).rssi.intValue());
        assertEquals(5, results.get(1).rssi.intValue());
        assertEquals(8, results.get(2).rssi.intValue());
        assertEquals(10, results.get(3).rssi.intValue());
        assertEquals(11, results.get(4).rssi.intValue());
    }

    @Test
    void testApplyingDifferentLimitingStrategiesToDifferentDevices() {
        final Properties properties = new Properties();
        properties.put("filter.mode", "whitelist");
        properties.put("tag.F1E2D3C4B5A6.limitingStrategy", "onMovement");
        Config.readConfigFromProperties(properties);

        final MainTest.MockConnection mockConnection = new MainTest.MockConnection();
        final LimitingStrategy testingStrategy = new TestingStrategy();
        final PersistenceService service = new PersistenceService(mockConnection, testingStrategy);

        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData.mac = "ABCDEF012345";
        final HCIData hciData2 = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData2.mac = "F1E2D3C4B5A6";

        setClockToMilliseconds(0);
        final DataFormatV3 v3 = new DataFormatV3();

        service.store(withAcceleration(withRssi(v3.handle(hciData), 1), 1d)); // Store: first instance
        service.store(withAcceleration(withRssi(v3.handle(hciData), 2), 1d)); // Not: no time passed
        setClockToMilliseconds(1000);
        service.store(withAcceleration(withRssi(v3.handle(hciData), 3), 1d)); // Not: still not enough time passed
        setClockToMilliseconds(3000);
        service.store(withAcceleration(withRssi(v3.handle(hciData), 4), 1d)); // Not: same as above
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 5), 1d)); // Store: first instance
        setClockToMilliseconds(6000);
        service.store(withAcceleration(withRssi(v3.handle(hciData), 6), 1.99d)); // Not: still not enough time passed; not using movement detection
        setClockToMilliseconds(9000);
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 7), 1.99d)); // Store: movement detected
        setClockToMilliseconds(10000);
        service.store(withAcceleration(withRssi(v3.handle(hciData), 8), 1d)); // Store: enough time has passed
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 9), 1d)); // Store: movement detected
        setClockToMilliseconds(11000);
        service.store(withAcceleration(withRssi(v3.handle(hciData), 10), 1d)); // Not: not enough time passed
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 11), 1d)); // Store: happens right after movement was detected
        setClockToMilliseconds(12000);
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 12), 1d)); // Not: no movement and not enough time passed
        setClockToMilliseconds(90000);
        service.store(withAcceleration(withRssi(v3.handle(hciData2), 13), 1d)); // Store: enough time passed

        final List<RuuviMeasurement> results = mockConnection.getMeasurements();
//        assertEquals(8, results.size());
        assertEquals(1, results.get(0).rssi.intValue());
        assertEquals(5, results.get(1).rssi.intValue());
        assertEquals(7, results.get(2).rssi.intValue());
        assertEquals(8, results.get(3).rssi.intValue());
        assertEquals(9, results.get(4).rssi.intValue());
        assertEquals(10, results.get(5).rssi.intValue());
        assertEquals(11, results.get(6).rssi.intValue());
        assertEquals(13, results.get(7).rssi.intValue());
    }

    private RuuviMeasurement withAcceleration(final RuuviMeasurement measurement, double accelerationX) {
        measurement.accelerationX = accelerationX;
        return measurement;
    }

    private void setClockToMilliseconds(final long millis) {
        TestFixture.setClockToMilliseconds(() -> millis);
    }

    private static RuuviMeasurement withRssi(final RuuviMeasurement measurement, final int rssi) {
        measurement.rssi = rssi;
        return measurement;
    }


    public static class TestingStrategy implements LimitingStrategy {

        @Override
        public Optional<RuuviMeasurement> apply(final RuuviMeasurement measurement) {
            if (Arrays.asList(1, 5, 8, 10, 11).contains(measurement.rssi)) {
                return Optional.of(measurement);
            }
            return Optional.empty();
        }
    }
}
