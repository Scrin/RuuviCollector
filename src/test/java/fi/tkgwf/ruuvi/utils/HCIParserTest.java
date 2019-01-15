package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HCIParserTest {

    @Test
    void assertAllFields() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getDataFormat3Message());
        System.out.println(hciData);
        assertEquals(4, hciData.packetType.intValue());
        assertEquals(62, hciData.eventCode.intValue());
        assertEquals(33, hciData.packetLength.intValue());
        assertEquals(2, hciData.subEvent.intValue());
        assertEquals(1, hciData.numberOfReports.intValue());
        assertEquals(3, hciData.eventType.intValue());
        assertEquals(1, hciData.peerAddressType.intValue());
        assertEquals("AABBCCDDEEFF", hciData.mac);
        assertEquals(-76, hciData.rssi.intValue());

        assertEquals(21, hciData.reports.get(0).length.intValue());
        assertEquals(1, hciData.reports.size());

        assertEquals(2, hciData.reports.get(0).advertisements.size());
        assertEquals(2, hciData.reports.get(0).advertisements.get(0).length.intValue());
        assertEquals(1, hciData.reports.get(0).advertisements.get(0).type.intValue());
        assertEquals(Arrays.asList((byte) 6), hciData.reports.get(0).advertisements.get(0).data);

        assertEquals(17, hciData.reports.get(0).advertisements.get(1).length.intValue());
        assertEquals(255, hciData.reports.get(0).advertisements.get(1).type.intValue());
        assertEquals(Arrays.asList((byte) -103, (byte) 4, (byte) 3, (byte) 73, (byte) 22, (byte) 14, (byte) -66,
            (byte) -8, (byte) 0, (byte) 5, (byte) -1, (byte) -22, (byte) 3, (byte) -31, (byte) 11, (byte) -65),
            hciData.reports.get(0).advertisements.get(1).data);
    }
}
