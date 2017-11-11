package fi.tkgwf.ruuvi.handler.impl;

import java.util.Base64;

public class DataFormatV4 extends AbstractEddystoneURL {

    @Override
    protected byte[] base64ToByteArray(String base64) {
        base64 = base64.substring(0, base64.length() - 1); // The extra character alone at the end makes the base64 string to be invalid, discard it
        return Base64.getDecoder().decode(base64.replace('-', '+').replace('_', '/')); // Ruuvi uses URL-safe Base64, convert that to "traditional" Base64
    }
}
