package fi.tkgwf.ruuvi.utils;

import org.apache.commons.lang3.StringUtils;

public abstract class RuuviUtils {

    /**
     * Converts a space-separated string of hex to ASCII
     *
     * @param hex space separated string of hex
     * @return the ASCII representation of the hex string
     */
    public static String hexToAscii(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 3) {
            sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return sb.toString();
    }

    /**
     * Converts a hex sequence to raw bytes
     *
     * @param hex the hex string to parse
     * @return a byte-array containing the byte-values of the hex string
     */
    public static byte[] hexToBytes(String hex) {
        String s = hex.replaceAll(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Gets a MAC address from a space-separated hex string, starting after the
     * prefix length
     *
     * @param line a space separated string of hex, first six decimals are
     * assumed to be part of the MAC address, rest of the line is discarded
     * @return the MAC address, without spaces
     */
    public static String getMacFromLine(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String[] split = line.split(" ", 7); // 6 blocks plus remaining garbage
        for (int i = 5; i >= 0; i--) {
            sb.append(split[i]);
        }
        return sb.toString();
    }
}
