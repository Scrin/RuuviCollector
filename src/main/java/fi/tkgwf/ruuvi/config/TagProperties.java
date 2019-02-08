package fi.tkgwf.ruuvi.config;

import fi.tkgwf.ruuvi.strategy.LimitingStrategy;
import fi.tkgwf.ruuvi.strategy.impl.DefaultDiscardingWithMotionSensitivityStrategy;

import java.util.Optional;

public class TagProperties {
    private final String mac;
    private final LimitingStrategy limitingStrategy;

    private TagProperties(final String mac, final LimitingStrategy limitingStrategy) {
        this.mac = mac;
        this.limitingStrategy = Optional.ofNullable(limitingStrategy)
            .orElse(Config.getLimitingStrategy());
    }

    public static TagProperties defaultValues() {
        return new TagProperties(null, Config.getLimitingStrategy());
    }

    public String getMac() {
        return mac;
    }

    public LimitingStrategy getLimitingStrategy() {
        return limitingStrategy;
    }

    public static Builder builder(final String mac) {
        return new Builder(mac);
    }


    public static class Builder {
        private String mac;
        private LimitingStrategy limitingStrategy;

        public Builder(final String mac) {
            this.mac = mac;
        }

        public Builder add(final String key, final String value) {
            if ("limitingStrategy".equals(key)) {
                if ("onMovement".equals(value)) {
                    this.limitingStrategy = new DefaultDiscardingWithMotionSensitivityStrategy();
                }
            }
            return this;
        }

        public TagProperties build() {
            return new TagProperties(mac, limitingStrategy);
        }
    }
}
