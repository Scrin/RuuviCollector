package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.config.ConfigTest;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFormatV3Test {

    @BeforeEach
    void resetConfigBefore() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

    @AfterAll
    static void resetConfigAfter() {
        Config.reload(ConfigTest.configTestFileFinder());
    }

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
    void canHandleShouldReturnTrueForMessagesOfRightFormat() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final boolean result = new DataFormatV3().canHandle(hciData);
        assertTrue(result);
    }

}
