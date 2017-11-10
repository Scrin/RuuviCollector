package fi.tkgwf.ruuvi.handler;

import fi.tkgwf.ruuvi.bean.RuuviMeasurement;

/**
 * Creates {@link InfluxDBData} instances from raw dump from hcidump. An
 * instance should keep an internal state of the line previously encountered, as
 * packets from hcidump may be split across multiple lines
 */
public interface BeaconHandler {

    /**
     * Reads a line of the raw dump from hcidump, and creates a
     * {@link RuuviMeasurement} from a BLE beacon sent by a tag when a complete
     * packet has been received.
     *
     * @param rawLine the raw dump line from hcidump process
     * @param mac the MAC address of the packet source
     * @return an instance of a {@link RuuviMeasurement}, or null if this
     * handler cannot understand the packet or the packet is not yet complete
     */
    RuuviMeasurement read(String rawLine, String mac);

    /**
     * Resets the internal state of this handler. This is called upon read
     * errors and other anomalies that may leave handlers in an inconsistent
     * state.
     */
    void reset();
}
