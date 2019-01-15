package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fi.tkgwf.ruuvi.TestFixture.RSSI_BYTE;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void integrationTest() {
        // Setup the test. Use two devices and change one variable for each hcidump line so that the messages
        // can be told apart at the end.

        final String hcidataOfDevice1 = TestFixture.getDataFormat3Message();
        final String hcidata2OfDevice2 = TestFixture.getDataFormat3Message()
            .replace("AA", "BB"); // Changing the MAC address

        final Main main = new Main();
        final BufferedReader reader = new BufferedReader(new StringReader(
            "Ignorable garbage at the start" + "\n"
                + hcidataOfDevice1.replace(RSSI_BYTE, "01") + "\n"
                + hcidataOfDevice1.replace(RSSI_BYTE, "02") + "\n"
                + hcidataOfDevice1.replace(RSSI_BYTE, "03") + "\n"
                + hcidata2OfDevice2.replace(RSSI_BYTE, "04") + "\n"
                + hcidata2OfDevice2.replace(RSSI_BYTE, "05") + "\n"
        ));

        // The following are the timestamps on which the hcidump lines above will be read.
        // By default (see Config.getMeasurementUpdateLimit()) a measurement is discarded
        // if it arrives less than 9900 milliseconds after the previous measurement from
        // the same device.
        setClockToMilliseconds(0L, 5000L, 10000L, 11000L, 12000L, 99999L);

        // Enough with the setup, run the process:

        final boolean runResult = main.run(reader);
        assertTrue(runResult);

        // Assert that only the expected measurements were persisted:

        final MockConnection mockConnection = (MockConnection) Config.getDBConnection();
        assertEquals(3, mockConnection.getMeasurements().size());
        assertEquals(1, mockConnection.getMeasurements().get(0).rssi.intValue());
        assertEquals(3, mockConnection.getMeasurements().get(1).rssi.intValue());
        assertEquals(4, mockConnection.getMeasurements().get(2).rssi.intValue());
        assertTrue(mockConnection.isCloseCalled());
    }

    private void setClockToMilliseconds(final Long... millis) {
        try {
            final Field clock = Config.class.getDeclaredField("clock");
            clock.setAccessible(true);
            clock.set(null, new FixedInstantsClock(Arrays.stream(millis)
                .map(Instant::ofEpochMilli).collect(toList()), ZoneId.of("UTC")));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static class MockConnection implements DBConnection {

        private final ArrayList<RuuviMeasurement> measurements = new ArrayList<>();
        private boolean closeCalled = false;

        @Override
        public void save(final RuuviMeasurement measurement) {
            this.measurements.add(measurement);
        }

        @Override
        public void close() {
            this.closeCalled = true;
        }

        List<RuuviMeasurement> getMeasurements() {
            return measurements;
        }

        boolean isCloseCalled() {
            return closeCalled;
        }
    }


    /**
     * A clock whose readings can be pre-programmed.
     */
    static final class FixedInstantsClock extends Clock implements Serializable {
        private final List<Instant> instants;
        private final ZoneId zone;
        private int readCount = 0;

        FixedInstantsClock(List<Instant> fixedInstants, ZoneId zone) {
            this.instants = fixedInstants;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(this.zone)) {
                return this;
            }
            return new FixedInstantsClock(instants, zone);
        }

        @Override
        public long millis() {
            final long millis = instants.get(readCount).toEpochMilli();
            readCount++;
            return millis;
        }

        @Override
        public Instant instant() {
            final Instant instant = instants.get(readCount);
            readCount++;
            return instant;
        }

        @Override
        public String toString() {
            return "CustomClock[" + instants + "," + zone + "]";
        }
    }
}
