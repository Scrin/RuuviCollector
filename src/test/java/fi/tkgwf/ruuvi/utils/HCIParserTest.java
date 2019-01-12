package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV2;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV4;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV5;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HCIParserTest {
    @Test
    void testMacAddressAndRssiParsing() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        System.out.println(hciData);
        assertEquals("AABBCCDDEEFF", hciData.mac);
        assertEquals(-76, hciData.rssi.intValue());

        System.out.println("2: " + new DataFormatV2().handle(hciData));
        System.out.println("3: " + new DataFormatV3().handle(hciData));
        System.out.println("4: " + new DataFormatV4().handle(hciData));
        System.out.println("5: " + new DataFormatV5().handle(hciData));
    }
}
