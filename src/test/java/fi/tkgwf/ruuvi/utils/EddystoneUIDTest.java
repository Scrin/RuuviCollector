// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.EddystoneUID;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.parser.EddystoneUIDParser;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EddystoneUIDTest {

    private static final Logger LOG = Logger.getLogger(EddystoneUIDTest.class);

    @Test
    void testEddystoneUID() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getEddystoneUIDMessage());
        LOG.debug(hciData);

        final BeaconHandler handler = new BeaconHandler();
        Optional<EnhancedRuuviMeasurement> measurement = handler.handle(hciData);
        assert(measurement.isPresent());
        LOG.info(measurement.get());

        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0x16);
        EddystoneUID eddystoneUID = EddystoneUIDParser.parse(adData.dataBytes());
        assertEquals("9E5D61E66621DA7CA630", eddystoneUID.getNamespaceID());
        assertEquals("0ED145673A89", eddystoneUID.getInstanceID());
        assertEquals(-109, eddystoneUID.getSignalPower().byteValue());
    }
}
