package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataFormatV5Test {
    @Test
    void handleShouldReturnNullForMessagesOfWrongFormat() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final RuuviMeasurement result = new DataFormatV5().handle(hciData);
        assertNull(result);
    }

    @Test
    void canHandleShouldReturnFalseForMessagesOfWrongFormat() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final boolean result = new DataFormatV5().canHandle(hciData);
        assertFalse(result);
    }
}
