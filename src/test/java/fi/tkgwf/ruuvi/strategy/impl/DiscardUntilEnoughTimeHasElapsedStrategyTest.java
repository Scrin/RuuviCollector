package fi.tkgwf.ruuvi.strategy.impl;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscardUntilEnoughTimeHasElapsedStrategyTest {
    @Test
    void testDiscardingOfMeasurementsUntilEnoughTimeHasPassedSincePreviousMeasurement() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final HCIData hciData2 = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData2.mac = "112233445566";

        setClockToMilliseconds(0);
        final DataFormatV3 v3 = new DataFormatV3();

        final DiscardUntilEnoughTimeHasElapsedStrategy strategy = new DiscardUntilEnoughTimeHasElapsedStrategy();
        assertTrue(strategy.apply(withRssi(v3.handle(hciData), 1)).isPresent());
        assertFalse(strategy.apply(withRssi(v3.handle(hciData), 2)).isPresent());
        setClockToMilliseconds(1000);
        assertFalse(strategy.apply(withRssi(v3.handle(hciData), 3)).isPresent());
        setClockToMilliseconds(3000);
        assertFalse(strategy.apply(withRssi(v3.handle(hciData), 4)).isPresent());
        assertTrue(strategy.apply(withRssi(v3.handle(hciData2), 5)).isPresent());
        setClockToMilliseconds(6000);
        assertFalse(strategy.apply(withRssi(v3.handle(hciData), 6)).isPresent());
        setClockToMilliseconds(9000);
        assertFalse(strategy.apply(withRssi(v3.handle(hciData), 7)).isPresent());
        setClockToMilliseconds(10000);
        assertTrue(strategy.apply(withRssi(v3.handle(hciData), 8)).isPresent());
        assertFalse(strategy.apply(withRssi(v3.handle(hciData2), 9)).isPresent());
        setClockToMilliseconds(90000);
        assertTrue(strategy.apply(withRssi(v3.handle(hciData), 10)).isPresent());
        assertTrue(strategy.apply(withRssi(v3.handle(hciData2), 11)).isPresent());
    }

    private static RuuviMeasurement withRssi(final RuuviMeasurement measurement, final int rssi) {
        measurement.rssi = rssi;
        return measurement;
    }

    private void setClockToMilliseconds(final long millis) {
        TestFixture.setClockToMilliseconds(() -> millis);
    }
}
