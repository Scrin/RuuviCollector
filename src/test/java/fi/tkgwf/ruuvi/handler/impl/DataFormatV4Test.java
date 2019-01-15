package fi.tkgwf.ruuvi.handler.impl;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.utils.HCIParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class DataFormatV4Test {
    @Test
    void shouldReturnNullForMessagesOfWrongFormat() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        final RuuviMeasurement result = new DataFormatV4().handle(hciData);
        assertNull(result);
    }
}
