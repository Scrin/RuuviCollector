package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceServiceTest {

    @Test
    void testDiscardingOfMeasurementsUntilEnoughTimeHasPassedSincePreviousMeasurement() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final HCIData hciData2 = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData2.mac = "112233445566";

        setClockToMilliseconds(0);
        final DataFormatV3 v3 = new DataFormatV3();

        final MainTest.MockConnection mockConnection = new MainTest.MockConnection();
        final PersistenceService service = new PersistenceService(mockConnection);
        service.store(withRssi(v3.handle(hciData), 1));
        service.store(withRssi(v3.handle(hciData), 2));
        setClockToMilliseconds(1000);
        service.store(withRssi(v3.handle(hciData), 3));
        setClockToMilliseconds(3000);
        service.store(withRssi(v3.handle(hciData), 4));
        service.store(withRssi(v3.handle(hciData2), 5));
        setClockToMilliseconds(6000);
        service.store(withRssi(v3.handle(hciData), 6));
        setClockToMilliseconds(9000);
        service.store(withRssi(v3.handle(hciData), 7));
        setClockToMilliseconds(10000);
        service.store(withRssi(v3.handle(hciData), 8));
        service.store(withRssi(v3.handle(hciData2), 9));
        setClockToMilliseconds(90000);
        service.store(withRssi(v3.handle(hciData), 10));
        service.store(withRssi(v3.handle(hciData2), 11));

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

    private void setClockToMilliseconds(final long millis) {
        try {
            final Field clock = Config.class.getDeclaredField("clock");
            clock.setAccessible(true);
            clock.set(null, Clock.fixed(Instant.ofEpochMilli(millis), ZoneId.of("UTC")));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
