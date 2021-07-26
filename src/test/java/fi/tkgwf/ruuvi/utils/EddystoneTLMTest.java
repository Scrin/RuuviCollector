// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.TestFixture;
import fi.tkgwf.ruuvi.bean.EddystoneTLM;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.handler.BeaconHandler;
import fi.tkgwf.ruuvi.parser.EddystoneTLMParser;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EddystoneTLMTest {

    private static final Logger LOG = Logger.getLogger(EddystoneTLMTest.class);

    @Test
    void testEddystoneTLM() {
        final HCIData hciData = new HCIParser().readLine(TestFixture.getEddystoneTMLMessage());
        LOG.debug(hciData);

        final BeaconHandler handler = new BeaconHandler();
        Optional<EnhancedRuuviMeasurement> measurement = handler.handle(hciData);
        assert(measurement.isPresent());
        LOG.info(measurement.get());

        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0x17);
        EddystoneTLM eddystoneTLM = EddystoneTLMParser.parse(adData.dataBytes());
        assertEquals(3.0, eddystoneTLM.getVBatt().doubleValue());
        assertEquals(25.0, eddystoneTLM.getTemp().doubleValue());
        assertEquals(1000, eddystoneTLM.getAdvCnt().intValue());
        assertEquals(1000, eddystoneTLM.getSecCnt().intValue());
    }
}
