package fi.tkgwf.ruuvi.handler.impl;

public class DataFormatV2 extends AbstractEddystoneURL {

    protected static final String RUUVI_BEGINS = "> 04 3E 2A 02 01 03 01 ";

    @Override
    protected String getRuuviBegins() {
        return RUUVI_BEGINS;
    }
}
