package fi.tkgwf.ruuvi.bean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class InfluxDBData {

    private List<Measurement> measurements = new LinkedList<>();

    private InfluxDBData(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    @Override
    public String toString() {
        return measurements.stream().map(Measurement::toString).collect(Collectors.joining("\n"));
    }

    public static class Builder {

        private final List<Measurement> measurements = new LinkedList<>();
        private final Map<String, String> tags;

        public Builder() {
            this.tags = new HashMap<>();
        }

        /**
         * @param mac MAC address of the tag
         * @return
         */
        public Builder mac(String mac) {
            return globalTag("mac", mac);
        }

        /**
         * @param protocolVersion Protocol version of the packet
         * @return
         */
        public Builder protocolVersion(String protocolVersion) {
            return globalTag("protocolVersion", protocolVersion);
        }

        /**
         * A tag to be appended to each measurement
         *
         * @param tagKey key for the tag
         * @param tagValue value for the tag
         * @return
         */
        public Builder globalTag(String tagKey, String tagValue) {
            this.tags.put(tagKey, tagValue);
            return this;
        }

        /**
         * Add new measurement
         *
         * @param measurement Name of the measurement
         * @return
         */
        public Measurement.Builder measurement(String measurement) {
            return new Measurement.Builder(this, measurements, measurement);
        }

        /**
         * Build an instance of {@link InfluxDBData}
         *
         * @return
         */
        public InfluxDBData build() {
            measurements.forEach(measurement -> measurement.tags.putAll(tags));
            return new InfluxDBData(measurements);
        }
    }

    private static class Measurement {

        public final String measurement;
        public final Map<String, String> tags;
        public final Number value;

        private Measurement(String measurement, Map<String, String> tags, Number value) {
            this.measurement = measurement;
            this.tags = tags;
            this.value = value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(measurement);
            tags.entrySet().stream().map(this::createTag).forEach(sb::append);
            sb.append(" value=").append(value);
            return sb.toString();
        }

        private String createTag(Entry<String, String> entry) {
            return String.format(",%s=%s", entry.getKey(), entry.getValue());
        }

        public static class Builder {

            private final InfluxDBData.Builder builder;
            private final List<Measurement> measurements;
            private final String measurement;
            private final Map<String, String> tags;

            private Builder(InfluxDBData.Builder builder, List<Measurement> measurements, String measurement) {
                this.builder = builder;
                this.measurements = measurements;
                this.measurement = measurement;
                this.tags = new HashMap<>();
            }

            /**
             * Add a tag to this measurement
             *
             * @param tagKey key for the tag
             * @param tagValue value for the tag
             * @return
             */
            public Builder tag(String tagKey, String tagValue) {
                this.tags.put(tagKey, tagValue);
                return this;
            }

            /**
             * Add a value to this measurement and finish setting it up
             *
             * @param value The value to set for this measurement
             * @return The underlying Builder
             */
            public InfluxDBData.Builder value(Number value) {
                if (StringUtils.isBlank(measurement)) {
                    throw new IllegalArgumentException("measurement can't be blank");
                }
                if (value == null) {
                    throw new IllegalArgumentException("Value must not be null");
                }
                measurements.add(new Measurement(measurement, tags, value));
                return builder;
            }
        }
    }
}
