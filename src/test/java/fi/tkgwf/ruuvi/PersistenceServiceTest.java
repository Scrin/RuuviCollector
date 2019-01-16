package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceServiceTest {

    @Test
    void testDiscardingOfMeasurementsUntilEnoughTimeHasPassedSincePreviousMeasurement() {
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
