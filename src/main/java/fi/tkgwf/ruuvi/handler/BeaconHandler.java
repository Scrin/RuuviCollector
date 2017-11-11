package fi.tkgwf.ruuvi.handler;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

/**
 * Creates {@link InfluxDBData} instances from raw dump from hcidump. An
 * instance should keep an internal state of the line previously encountered, as
 * packets from hcidump may be split across multiple lines
 */
public interface BeaconHandler {

    /**
     * Handles a packet and creates a {@link RuuviMeasurement} if the handler
     * understands this packet.
     *
     * @param hciData the data parsed from hcidump
     * @return an instance of a {@link RuuviMeasurement}, or null if this
     * handler cannot understand the packet or the packet is not yet complete
     */
    RuuviMeasurement handle(HCIData hciData);

    /**
     * Resets the internal state of this handler. This is called upon read
     * errors and other anomalies that may leave handlers in an inconsistent
     * state.
     */
    default void reset() {
    }
}
