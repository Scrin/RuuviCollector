package fi.tkgwf.ruuvi;

import fi.tkgwf.ruuvi.config.Config;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class TestFixture {
    public static final String RSSI_BYTE = "B4";

    /**
     * Refer to https://github.com/ruuvi/ruuvi-sensor-protocols for the full specification
     * @return An example message in data format 2, like hcidump would return it.
     */
    public static String getDataFormat3Message() {
//                                     MAC address (6 bytes)
//                                     |                                   Manufacturer ID 0x0499
//                                     |                                   |     Data format definition (3)
//                                     |                                   |     |  Humidity x2
//                                     |                                   |     |  |  Temperature (1st byte: signed integer; 2nd byte: decimals)
//                                     |                                   |     |  |  |     Pressure (2 bytes)
//                                     |                                   |     |  |  |     |     Acceleration-X (2 bytes)
//                                     |                                   |     |  |  |     |     |     Acceleration-Y (2 bytes)
//                                     |                                   |     |  |  |     |     |     |     Acceleration-Z (2 bytes)
//                                     |                                   |     |  |  |     |     |     |     |     Battery voltage (2 bytes)
//                                     |                                   |     |  |  |     |     |     |     |     |     RSSI
//                                     |                                   |     |  |  |     |     |     |     |     |     |
        return "> 04 3E 21 02 01 03 01 FF EE DD CC BB AA 15 02 01 06 11 FF 99 04 03 49 16 0E BE F8 00 05 FF EA 03 E1 0B BF B4";
    }

    public static String getIBeaconMessage() {
//
//                                     MAC address (6 bytes)               Manufacturer ID 0x004C
//                                     |                                   |     Type (iBeacon)
//                                     |                                   |     |  Length
//                                     |                                   |     |  |  UUID (16 bytes)                                 Major (2 bytes)
//                                     |                                   |     |  |  |                                               |     Minor (2 bytes)
//                                     |                                   |     |  |  |                                               |     |     Calibrated Tx power at 1 m
//                                     |                                   |     |  |  |                                               |     |     |
        return "> 04 3E 21 02 01 03 01 FF EE DD CC BB AA 1E 02 01 06 1A FF 4C 00 02 15 FD A5 06 93 A4 E2 4F B1 AF CF C6 EB 07 64 78 25 00 01 00 02 C5";
    }

    public static String getEddystoneUIDMessage() {
//
//                                     MAC address (6 bytes)         AD1 Length
//                                     |                             |  AD1 Type (03 = Service UUID)
//                                     |                             |  |  Eddystone Service UUID (2 bytes)
//                                     |                             |  |  |     AD2 Length
//                                     |                             |  |  |     |  AD2 Type (16 = Service Data)
//                                     |                             |  |  |     |  |  Eddystone Service UUID (2 bytes)
//                                     |                             |  |  |     |  |  |     Eddystone Frame Type (03 = UID)
//                                     |                             |  |  |     |  |  |     |  Calibrated Tx power at 0 m
//                                     |                             |  |  |     |  |  |     |  |  Namespace ID (10 bytes)       Instance ID (6 bytes)
//                                     |                             |  |  |     |  |  |     |  |  |                             |                 RFU (2 bytes)
//                                     |                             |  |  |     |  |  |     |  |  |                             |                 |
        return "> 04 3E 21 02 01 03 01 FF EE DD CC BB AA 1F 02 01 06 03 03 AA FE 17 16 AA FE 00 93 9E 5D 61 E6 66 21 DA 7C A6 30 0E D1 45 67 3A 89 00 00";
    }

    public static String getEddystoneTMLMessage() {
//
//                                     MAC address (6 bytes)         AD1 Length
//                                     |                             |  AD1 Type (03 = Service UUID)
//                                     |                             |  |  UUID (2 bytes)
//                                     |                             |  |  |     AD2 Length
//                                     |                             |  |  |     |  AD2 Type (10 = Service Data)
//                                     |                             |  |  |     |  |  UUID (2 bytes)
//                                     |                             |  |  |     |  |  |     Eddystone Frame Type
//                                     |                             |  |  |     |  |  |     |  TLM version (value = 0x00)
//                                     |                             |  |  |     |  |  |     |  |  VBAT  TEMP  ADV_CNT     SEC_CNT
//                                     |                             |  |  |     |  |  |     |  |  |     |     |           |
        return "> 04 3E 21 02 01 03 01 FF EE DD CC BB AA 1A 02 01 06 03 03 AA FE 11 17 AA FE 20 00 0B B8 19 00 00 00 03 E8 00 00 27 10";
    }

    public static void setClockToMilliseconds(final Supplier<Long> timestampSupplier) {
        try {
            final Field clock = Config.class.getDeclaredField("timestampProvider");
            clock.setAccessible(true);
            clock.set(null, timestampSupplier);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
