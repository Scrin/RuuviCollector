package fi.tkgwf.ruuvi.handler.impl;

public class DataFormatV4 extends AbstractEddystoneURL {

    protected static final String RUUVI_BEGINS = "> 04 3E 2B 02 01 03 01 ";

    @Override
    protected String getRuuviBegins() {
        return RUUVI_BEGINS;
    }

    @Override
    protected byte[] base64ToByteArray(String base64) {
        // The extra character alone at the end makes the base64 string to be invalid, discard it
        return super.base64ToByteArray(base64.substring(0, base64.length() - 1));
    }
}
