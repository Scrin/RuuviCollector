// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.parser;

import fi.tkgwf.ruuvi.bean.IBeacon;
import java.util.UUID;
import java.nio.ByteBuffer;

public class IBeaconParser {

    private final static int[] RUUVI_COMPANY_IDENTIFIER = { 0x4C, 0x00 };

    public static IBeacon parse(byte[] data) {

        if (data.length < 2 || (data[0] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[0]|| (data[1] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[1]) {
            return null;
        }

        if (data.length < 25 || data[2] != 0x02 || data[3] != 0x15) {
            return null;
        }

        IBeacon iBeacon = new IBeacon();

        byte[] byteUUID = new byte[16];
        for (int i = 0; i < 16; i++) {
            byteUUID[i] = data[i+4];
        }
        ByteBuffer bb = ByteBuffer.wrap(byteUUID);

        iBeacon.setUUID(new UUID(bb.getLong(), bb.getLong()));
        iBeacon.setMajor(((data[20] & 0xFF) << 8) | (data[21] & 0xFF));
        iBeacon.setMinor(((data[22] & 0xFF) << 8) | (data[23] & 0xFF));
        iBeacon.setSignalPower(data[24]);

        return iBeacon;
    }
}
