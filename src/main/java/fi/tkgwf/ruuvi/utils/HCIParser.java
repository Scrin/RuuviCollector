package fi.tkgwf.ruuvi.utils;

import fi.tkgwf.ruuvi.bean.HCIData;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

/**
 * This class is capable of parsing the raw format dump from hcidump (output of
 * command "hcidump --raw"). This implementation is a state-machine, and thus
 * it's not thread safe.
 */
public class HCIParser {

    private boolean sendingData;
    private int indexInPacket;
    private int indexInReport;
    private int indexInADData;
    private int processedReports;
    private HCIData data;
    private HCIData.Report report;
    private HCIData.Report.AdvertisementData adData;

    public HCIParser() {
        reset();
    }

    /**
     * Reads lines from hcidump raw output and returns a HCIData instance when
     * it's ready. Long packets are split to multiple lines in the hcidump raw
     * output.
     *
     * @param line raw line from hcidump --raw output
     * @return An instance of HCIData containing the parsed data from this line
     * and the previous ones
     */
    public HCIData readLine(String line) {
        if (StringUtils.isBlank(line)) {
            return null; // ignore blank lines
        }
        line = line.trim();
        if (line.charAt(0) == '>') { // new incoming packet begins
            reset();
            line = line.substring(1).trim(); // discard the > char
        }
        if (line.charAt(0) == '<') { // new outgoing packet begins
            sendingData = true;
        }
        if (sendingData) {
            return null; // currently reading a packet that is being sent rather than received, ignore it
        }
        byte[] lineData = Utils.hexToBytes(line);
        int i = 0;
        for (; i < lineData.length; i++, indexInPacket++) {
            handleByte(lineData[i]);
        }
        // the packet length is actually the length AFTER the length byte, which is the 3rd byte
        if (data.packetLength != null && indexInPacket >= data.packetLength + 3) {
            return data;
        } else {
            return null;
        }
    }

    private void reset() {
        sendingData = false;
        indexInPacket = 0;
        indexInReport = 0;
        indexInADData = 0;
        processedReports = 0;
        data = new HCIData();
    }

    private void handleByte(byte b) {
        switch (indexInPacket) {
            case 0:
                data.packetType = unsigned(b);
                break;
            case 1:
                data.eventCode = unsigned(b);
                break;
            case 2:
                data.packetLength = unsigned(b);
                break;
            case 3:
                data.subEvent = unsigned(b);
                break;
            case 4:
                data.numberOfReports = unsigned(b);
                break;
            case 5:
                data.eventType = unsigned(b);
                break;
            case 6:
                data.peerAddressType = unsigned(b);
                break;
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                if (data.mac == null) {
                    data.mac = "";
                }
                data.mac = String.format("%02X", b) + data.mac; // the MAC is "backwards"
                break;
            default:
                if (processedReports < data.numberOfReports) {
                    handleReport(b);
                } else {
                    data.rssi = (int) b;
                }
                break;
        }
    }

    private void handleReport(byte b) {
        if (indexInReport == 0) {
            report = new HCIData.Report();
            report.length = unsigned(b);
            if (data.reports == null) {
                data.reports = new ArrayList<>();
            }
            data.reports.add(report);
        } else {
            handleAdvertisementData(b);
        }
        indexInReport++;
        // Report length does not count the length byte itself
        if (indexInReport >= report.length + 1) {
            indexInReport = 0;
            report = null;
            processedReports++;
        }
    }

    private void handleAdvertisementData(byte b) {
        switch (indexInADData) {
            case 0:
                adData = new HCIData.Report.AdvertisementData();
                adData.length = unsigned(b);
                if (report.advertisements == null) {
                    report.advertisements = new ArrayList<>();
                }
                report.advertisements.add(adData);
                break;
            case 1:
                adData.type = unsigned(b);
                break;
            default:
                if (adData.data == null) {
                    adData.data = new ArrayList<>();
                }
                adData.data.add(b);
                break;
        }
        indexInADData++;
        // AD data length does not count the length byte itself
        if (indexInADData >= adData.length + 1) {
            indexInADData = 0;
            adData = null;
        }
    }

    private int unsigned(byte b) {
        return b & 0xFF;
    }
}
