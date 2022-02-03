package fi.tkgwf.ruuvi.handler;

import fi.tkgwf.ruuvi.bean.EnhancedRuuviMeasurement;
import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.IBeacon;
import fi.tkgwf.ruuvi.bean.EddystoneTLM;
import fi.tkgwf.ruuvi.bean.EddystoneUID;
import fi.tkgwf.ruuvi.common.bean.RuuviMeasurement;
import fi.tkgwf.ruuvi.common.parser.DataFormatParser;
import fi.tkgwf.ruuvi.common.parser.impl.AnyDataFormatParser;
import fi.tkgwf.ruuvi.config.Config;
import fi.tkgwf.ruuvi.parser.IBeaconParser;
import fi.tkgwf.ruuvi.parser.EddystoneTLMParser;
import fi.tkgwf.ruuvi.parser.EddystoneUIDParser;
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
                adData = hciData.findAdvertisementDataByType(0x17); // Eddystone tlm
                if (adData == null) {
                    return Optional.empty();
                }
            }
        }

        if (adData.dataBytes()[0] == (byte) 0x99 && adData.dataBytes()[1] == (byte) 0x04) {
            RuuviMeasurement measurement = parser.parse(adData.dataBytes());
            if (measurement == null) {
                return Optional.empty();
            }

            EnhancedRuuviMeasurement enhancedMeasurement = new EnhancedRuuviMeasurement(measurement);
            enhancedMeasurement.setMac(hciData.mac);
            enhancedMeasurement.setRssi(hciData.rssi);
            enhancedMeasurement.setName(Config.getTagName(hciData.mac));
            enhancedMeasurement.setReceiver(Config.getReceiver());
            return Optional.of(enhancedMeasurement);
        } else if (adData.dataBytes()[0] == (byte) 0x4C && adData.dataBytes()[1] == (byte) 0x00 && adData.dataBytes()[2] == (byte) 0x02 && adData.dataBytes()[3] == (byte) 0x15) {
            IBeacon beacon = IBeaconParser.parse(adData.dataBytes());
            if (beacon == null) {
                return Optional.empty();
            }

            EnhancedRuuviMeasurement enhancedMeasurement = new EnhancedRuuviMeasurement();
            enhancedMeasurement.setMac(hciData.mac);
            enhancedMeasurement.setRssi(hciData.rssi);
            enhancedMeasurement.setName(Config.getTagName(hciData.mac));
            enhancedMeasurement.setReceiver(Config.getReceiver());
            return Optional.of(enhancedMeasurement);
        } else if ((adData.dataBytes()[0] == (byte) 0xAA) && (adData.dataBytes()[1] == (byte) 0xFE) && (adData.dataBytes()[2] == (byte) 0x00)) {
            EddystoneUID eddystoneUID = EddystoneUIDParser.parse(adData.dataBytes());
            if (eddystoneUID == null) {
                return Optional.empty();
            }

            EnhancedRuuviMeasurement enhancedMeasurement = new EnhancedRuuviMeasurement();
            enhancedMeasurement.setMac(hciData.mac);
            enhancedMeasurement.setRssi(hciData.rssi);
            enhancedMeasurement.setName(Config.getTagName(hciData.mac));
            enhancedMeasurement.setReceiver(Config.getReceiver());
            return Optional.of(enhancedMeasurement);
        } else if ((adData.dataBytes()[0] == (byte) 0xAA) && (adData.dataBytes()[1] == (byte) 0xFE) && (adData.dataBytes()[2] == (byte) 0x20)) {
            EddystoneTLM eddystoneTLM = EddystoneTLMParser.parse(adData.dataBytes());
            if (eddystoneTLM == null) {
                return Optional.empty();
            }

            RuuviMeasurement measurement = new RuuviMeasurement();
            measurement.setBatteryVoltage(eddystoneTLM.getVBatt());
            measurement.setTemperature(eddystoneTLM.getTemp());

            EnhancedRuuviMeasurement enhancedMeasurement = new EnhancedRuuviMeasurement(measurement);
            enhancedMeasurement.setMac(hciData.mac);
            enhancedMeasurement.setRssi(hciData.rssi);
            enhancedMeasurement.setName(Config.getTagName(hciData.mac));
            enhancedMeasurement.setReceiver(Config.getReceiver());
            return Optional.of(enhancedMeasurement);
        }
        return Optional.empty();
    }
}
