package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.config.ConfigTest;
import fi.tkgwf.ruuvi.db.DBConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static fi.tkgwf.ruuvi.TestFixture.RSSI_BYTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @BeforeEach
    void resetConfigBefore() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @AfterAll
    static void restoreClock() {
        Config.reload(ConfigTest.configTestFileFinder());
        TestFixture.setClockToMilliseconds(System::currentTimeMillis);
    }

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
        assertEquals(1, mockConnection.getMeasurements().get(0).getRssi().intValue());
        assertEquals(3, mockConnection.getMeasurements().get(1).getRssi().intValue());
        assertEquals(4, mockConnection.getMeasurements().get(2).getRssi().intValue());
        assertTrue(mockConnection.isCloseCalled());
    }

    private void setClockToMilliseconds(final Long... millis) {
        TestFixture.setClockToMilliseconds(new FixedInstantsProvider(Arrays.asList(millis)));
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


    /**
     * A timestamp supplier whose readings can be pre-programmed.
     */
    static final class FixedInstantsProvider implements Supplier<Long> {
        private final List<Long> instants;
        private int readCount = 0;

        FixedInstantsProvider(List<Long> fixedInstants) {
            this.instants = fixedInstants;
        }

        @Override
        public Long get() {
            final long millis = instants.get(readCount);
            readCount++;
            return millis;
        }
    }
}
