package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.db.DBConnection;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void integrationTest() {
        final String hcidata = TestFixture.getDataFormat3Message();

        final Main main = new Main();
        main.run(new BufferedReader(new StringReader(hcidata)));

        final MockConnection mockConnection = (MockConnection) Config.getDBConnection();
        assertEquals(1, mockConnection.getMeasurements().size());
        assertEquals(22.14d, mockConnection.getMeasurements().get(0).temperature.doubleValue());
        assertTrue(mockConnection.isCloseCalled());
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
}
