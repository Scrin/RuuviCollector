// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.IBeacon;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.parser.IBeaconParser;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IBeaconTest {

    private static final Logger LOG = Logger.getLogger(IBeaconTest.class);

    @Test
    void testIBeacon() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getIBeaconMessage());
        LOG.debug(hciData);

        final BeaconHandler handler = new BeaconHandler();
        Optional<EnhancedRuuviMeasurement> measurement = handler.handle(hciData);
        assert(measurement.isPresent());
        LOG.info(measurement.get());

        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF); // Manufacturer-specific data, raw dataformats
        IBeacon iBeacon = IBeaconParser.parse(adData.dataBytes());
        assertEquals("fda50693-a4e2-4fb1-afcf-c6eb07647825", iBeacon.getUUID().toString());
        assertEquals(1, iBeacon.getMajor().intValue());
        assertEquals(2, iBeacon.getMinor().intValue());
        assertEquals(-59, iBeacon.getSignalPower().byteValue());
    }
}
