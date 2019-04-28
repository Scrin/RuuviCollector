package fi.tkgwf.ruuvi.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class BeaconHandlersTest {
    @Test
    void handleShouldReturnNullForMessagesOfWrongFormat() {
        List<BeaconHandlerInterface> handlers = BeaconHandlers.INSTANCE.getHandlers();
        assertEquals(4,handlers.size());
    }
}