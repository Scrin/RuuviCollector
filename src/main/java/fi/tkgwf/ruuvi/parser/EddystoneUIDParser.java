// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.parser;

import fi.tkgwf.ruuvi.bean.EddystoneUID;

public class EddystoneUIDParser {

    private final static int[] RUUVI_COMPANY_IDENTIFIER = { 0xAA, 0xFE };

    public static EddystoneUID parse(byte[] data) {

        if (data.length < 2 || (data[0] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[0]|| (data[1] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[1]) {
            return null;
        }

        if (data.length < 22 || data[2] != 0x00) {
            return null;
        }

        EddystoneUID eddystoneUID = new EddystoneUID();

        eddystoneUID.setSignalPower(data[3]);

        StringBuilder strNamespaceID = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            strNamespaceID.append(String.format("%02X", data[i+4]));
        }
        eddystoneUID.setNamespaceID(strNamespaceID.toString());

        StringBuilder strInstanceID = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            strInstanceID.append(String.format("%02X", data[i+14]));
        }
        eddystoneUID.setInstanceID(strInstanceID.toString());

        return eddystoneUID;
    }
}
