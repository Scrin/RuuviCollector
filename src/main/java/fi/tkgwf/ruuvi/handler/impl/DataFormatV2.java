package fi.tkgwf.ruuvi.handler.impl;

import java.util.Base64;

public class DataFormatV2 extends AbstractEddystoneURL {

    @Override
    protected byte[] base64ToByteArray(String base64) {
        return Base64.getDecoder().decode(base64.replace('-', '+').replace('_', '/')); // Ruuvi uses URL-safe Base64, convert that to "traditional" Base64
    }
}
