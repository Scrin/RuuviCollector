package fi.tkgwf.ruuvi.handler;

import fi.tkgwf.ruuvi.bean.HCIData;
import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

/**
 * Creates {@link RuuviMeasurement} instances from raw dumps from hcidump.
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

    /**
     * Tells whether this handler understands this packet.
     *
     * @param hciData the data parsed from hcidump
     * @return {@code true} if this handler understands the given data, {@code false} if not.
     */
    boolean canHandle(HCIData hciData);
}
