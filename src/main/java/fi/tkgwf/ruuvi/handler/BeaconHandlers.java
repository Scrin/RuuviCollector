package fi.tkgwf.ruuvi.handler;

import java.util.LinkedList;
import java.util.List;

import fi.tkgwf.ruuvi.handler.impl.DataFormatV2;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV3;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV4;
import fi.tkgwf.ruuvi.handler.impl.DataFormatV5;

/**
 * Enum/Singleton for a list of data format handlers.
 */
public enum BeaconHandlers {
    
    INSTANCE;
    private final List<BeaconHandlerInterface> beaconHandlers = new LinkedList<>();

    /**
     * Private Constructor
     */
    private BeaconHandlers() {
        beaconHandlers.add(new DataFormatV2());
        beaconHandlers.add(new DataFormatV3());
        beaconHandlers.add(new DataFormatV4());
        beaconHandlers.add(new DataFormatV5());
    }

    /**
     * Simple function to return the list of BeaconHandlers for the different data formats
     * @return List of {@link BeaconHandlerInterface}
     */
    public List<BeaconHandlerInterface> getHandlers() {
        return beaconHandlers;
    }
}