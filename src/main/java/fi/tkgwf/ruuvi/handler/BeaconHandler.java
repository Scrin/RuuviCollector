package fi.tkgwf.ruuvi.handler;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.common.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.common.parser.DataFormatParser;
import fi.tkgwf.ruuvi.common.parser.impl.AnyDataFormatParser;
import fi.tkgwf.ruuvi.config.Config;
import java.util.Optional;

/**
 * Creates {@link RuuviMeasurement} instances from raw dumps from hcidump.
 */
public class BeaconHandler {

    private final DataFormatParser parser = new AnyDataFormatParser();

    /**
     * Handles a packet and creates a {@link RuuviMeasurement} if the handler
     * understands this packet.
     *
     * @param hciData the data parsed from hcidump
     * @return an instance of a {@link EnhancedRuuviMeasurement} if this handler can
     * parse the packet
     */
    public Optional<EnhancedRuuviMeasurement> handle(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF); // Manufacturer-specific data, raw dataformats
        if (adData == null) {
            adData = hciData.findAdvertisementDataByType(0x16); // Eddystone url
            if (adData == null) {
                return null;
            }
        }
        RuuviMeasurement measurement = parser.parse(adData.dataBytes());
        if (measurement == null) {
            return Optional.empty();
        }
        EnhancedRuuviMeasurement enhancedMeasurement = new EnhancedRuuviMeasurement(measurement);
        enhancedMeasurement.setMac(hciData.mac);
        enhancedMeasurement.setRssi(hciData.rssi);
        enhancedMeasurement.setName(Config.getTagName(hciData.mac));
        return Optional.of(enhancedMeasurement);
    }
}
