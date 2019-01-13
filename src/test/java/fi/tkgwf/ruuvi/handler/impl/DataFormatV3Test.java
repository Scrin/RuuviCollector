package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataFormatV3Test {
    @Test
    void testThatHcidumpMessageOfDataFormatV3IsParsedCorrectly() {
        final RuuviMeasurement expected = new RuuviMeasurement();
        expected.mac = "AABBCCDDEEFF";
        expected.dataFormat = 3;
        expected.temperature = 22.14;
        expected.humidity = 36.5;
        expected.pressure = 98888.0;
        expected.accelerationX = 0.005;
        expected.accelerationY = -0.022;
        expected.accelerationZ = 0.993;
        expected.batteryVoltage = 3.007;
        expected.rssi = -76;

        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final RuuviMeasurement result = new DataFormatV3().handle(hciData);
        assertEquals(expected, result);
    }

    @Test
    void testDiscardingOfMeasurementsUntilEnoughTimeHasPassedSincePreviousMeasurement() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final HCIData hciData2 = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        hciData2.mac = "112233445566";

        setClockToMilliseconds(0);
        final DataFormatV3 dataFormat = new DataFormatV3();
        assertNotNull(dataFormat.handle(hciData));
        assertNull(dataFormat.handle(hciData));
        setClockToMilliseconds(1000);
        assertNull(dataFormat.handle(hciData));
        setClockToMilliseconds(3000);
        assertNull(dataFormat.handle(hciData));
        assertNotNull(dataFormat.handle(hciData2));
        setClockToMilliseconds(6000);
        assertNull(dataFormat.handle(hciData));
        setClockToMilliseconds(9000);
        assertNull(dataFormat.handle(hciData));
        setClockToMilliseconds(10000);
        assertNotNull(dataFormat.handle(hciData));
        assertNull(dataFormat.handle(hciData2));
        setClockToMilliseconds(90000);
        assertNotNull(dataFormat.handle(hciData));
        assertNotNull(dataFormat.handle(hciData2));
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
