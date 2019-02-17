package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

public class TagProperties {
    private final String mac;
    private final LimitingStrategy limitingStrategy;
    private final Predicate<String> influxDbFieldFilter;

    private TagProperties(final String mac, final LimitingStrategy limitingStrategy, final Predicate<String> influxFieldFilter) {
        this.mac = mac;
        this.limitingStrategy = Optional.ofNullable(limitingStrategy)
            .orElse(Config.getLimitingStrategy());
        this.influxDbFieldFilter = Optional.ofNullable(influxFieldFilter)
            .orElse(Config.getAllowedInfluxDbFieldsPredicate());
    }

    public static TagProperties defaultValues() {
        return new TagProperties(null,
            Config.getLimitingStrategy(),
            Config.getAllowedInfluxDbFieldsPredicate());
    }

    public String getMac() {
        return mac;
    }

    public LimitingStrategy getLimitingStrategy() {
        return limitingStrategy;
    }

    public Predicate<String> getInfluxDbFieldFilter() {
        return influxDbFieldFilter;
    }

    public static Builder builder(final String mac) {
        return new Builder(mac);
    }


    public static class Builder {
        private String mac;
        private LimitingStrategy limitingStrategy;
        private String storageValues;
        private Collection<String> storageValuesList = new HashSet<>();

        public Builder(final String mac) {
            this.mac = mac;
        }

        public Builder add(final String key, final String value) {
            if ("limitingStrategy".equals(key)) {
                if ("onMovement".equals(value)) {
                    this.limitingStrategy = new DefaultDiscardingWithMotionSensitivityStrategy();
                }
            } else if ("storage.values".equals(key)) {
                this.storageValues = value;
            } else if ("storage.values.list".equals(key)) {
                this.storageValuesList = Config.parseFilterInfluxDbFields(value);
            }
            return this;
        }

        public TagProperties build() {
            return new TagProperties(mac, limitingStrategy,
                Config.createInfluxDbFieldFilter(storageValues, storageValuesList));
        }
    }
}
