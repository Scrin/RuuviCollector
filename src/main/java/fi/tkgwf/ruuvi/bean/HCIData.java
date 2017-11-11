package fi.tkgwf.ruuvi.bean;

import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Parsed data from hcidump
 */
public class HCIData {

    public Integer packetType;
    public Integer eventCode;
    public Integer packetLength;
    public Integer subEvent;
    public Integer numberOfReports;
    public Integer eventType;
    public Integer peerAddressType;
    public String mac;
    public List<Report> reports;
    public Integer rssi;

    public Report.AdvertisementData findAdvertisementDataByType(int type) {
        if (reports == null) {
            return null;
        }
        return reports.stream()
                .filter(r -> r.advertisements != null)
                .flatMap(r -> r.advertisements.stream())
                .filter(a -> a.type != null)
                .filter(a -> a.type == type)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "HCIData{" + "packetType=" + packetType + ", eventCode=" + eventCode + ", packetLength=" + packetLength + ", subEvent=" + subEvent + ", numberOfReports=" + numberOfReports + ", eventType=" + eventType + ", peerAddressType=" + peerAddressType + ", mac=" + mac + ", reports=" + reports + ", rssi=" + rssi + '}';
    }

    public static class Report {

        public Integer length;
        public List<AdvertisementData> advertisements;

        @Override
        public String toString() {
            return "Report{" + "length=" + length + ", advertisements=" + advertisements + '}';
        }

        public static class AdvertisementData {

            public Integer length;
            public Integer type;
            public List<Byte> data;

            public byte[] dataBytes() {
                if (data == null) {
                    return new byte[0];
                } else {
                    return ArrayUtils.toPrimitive(data.toArray(new Byte[data.size()]));
                }
            }

            @Override
            public String toString() {
                return "AdvertisementData{" + "length=" + length + ", type=" + type + ", data=" + data + '}';
            }
        }
    }
}
