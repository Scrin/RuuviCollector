// Copyright 2021 Siemens Mobility GmbH

package fi.tkgwf.ruuvi.parser;

import fi.tkgwf.ruuvi.bean.EddystoneTLM;

public class EddystoneTLMParser {

    private final static int[] RUUVI_COMPANY_IDENTIFIER = { 0xAA, 0xFE };

    public static EddystoneTLM parse(byte[] data) {

        if (data.length < 2 || (data[0] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[0]|| (data[1] & 0xFF) != RUUVI_COMPANY_IDENTIFIER[1]) {
            return null;
        }

        if (data.length < 16 || data[2] != 0x20 || data[3] != 0x00) {
            return null;
        }

        EddystoneTLM eddystoneTLM = new EddystoneTLM();

        eddystoneTLM.setVBatt((((data[4] & 0xFF) << 8) | (data[5] & 0xFF)) / 1000d);

        int temperatureSign = (data[7] >> 7) & 1;
        int temperatureBase = (data[6] & 0x7F);
        double temperatureFraction = ((float) data[7]) / 100d;
        eddystoneTLM.setTemp(temperatureBase + temperatureFraction);
        if (temperatureSign == 1) {
            eddystoneTLM.setTemp(eddystoneTLM.getTemp() * -1);
        }

        StringBuilder strAdvCnt = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            strAdvCnt.append(String.format("%02X", data[i+8]));
        }
        eddystoneTLM.setAdvCnt(Integer.parseInt(strAdvCnt.toString(),16));

        StringBuilder strSecCnt = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            strSecCnt.append(String.format("%02X", data[i+12]));
        }
        eddystoneTLM.setSecCnt((Integer.parseInt(strSecCnt.toString(),16)) / 10);

        return eddystoneTLM;
    }
}
