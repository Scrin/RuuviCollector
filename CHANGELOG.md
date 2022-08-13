# Changelog

### v0.2.9

-   Update ruuvitag-common-java dependency to v1.0.2 to fix a bug in N/A field detection
-   Added sample service and setup scripts and related documentation (thanks anon8675309)

### v0.2.8

-   Added support for triggering a program exit in case of connection loss to InfluxDB (thanks anon8675309)

### v0.2.7

-   Added support for ibeacon and eddystone uid/tlm beacon formats (thanks Julia Goudsmith and Mustafa Yuecel (Siemens Mobility))
-   Added a configurable "receiver" tag (thanks Erkki Seppälä)
-   Added prometheus backend (thanks Joe Kearney)

### v0.2.6

-   Fix UTF-8 support in config files (ruuvi-collector.properties and ruuvi-names.properties). This might be a breaking change if your config relied on broken charset support.

### v0.2.5

-   Improved error tolerance while parsing for raw data
-   General refactoring with no functional changes
-   Added "named" filter mode which saves only named tags (thanks matthewgardner)

### v0.2.4

-   Significant amount of refactoring, this release may be a bit more unstable than usual
-   Added "defaultWithMotionSensitivity" limiting strategy (see ruuvi-collector.properties.example for configuration details, thanks ZeroOne3010)
-   Updated various dependencies and added [ruuvitag-common-java](https://github.com/Scrin/ruuvitag-common-java) as a dependency
-   Added tests (thanks ZeroOne3010)
-   Fixed a rare soft-crash when the raw hcidump line is invalid and cuts out in the middle of the MAC address

### v0.2.3

-   Added advanced configuration options for InfluxDB

### v0.2.2

-   Fixed a rare crash when hcidump was returning invalid data

### v0.2.1

-   Support for data format 5 ("RAW v2")

## v0.2.0

-   Major refactoring in the application logic
-   Changed the preferred format how data is saved to InfluxDB
-   Added new configuration properties and changed some old ones
-   Added the ability to give (human readable) friendly names for tags

##### --- Migrating from a pre-v.0.2.0 version:

Versions prior to v0.2.0 use single-value measurements, v0.2.0 uses multi-value measurements by default (for a limited time you can use the legacy format by changing that in the config). To migrate from a version prior to v0.2.0:

-   If you are using a custom config, copy the new ruuvi-collector.properties.example and replace your existing ruuvi-collector.properties and change the values you need. Some properties have their names changed and some new ones are added.
-   If you want to give names to your tags (in your existing measurements as well as future measurements), copy the ruuvi-names.properties.example to ruuvi-names.properties (in the same directory) as the collector and edit the file accordingly.
-   Run the collector with `migrate` parameter to migrate existing data to the new format: `java -jar ruuvi-collector-*.jar migrate` and let it run, this may take a long time if you have a lot of data or have a slow system (ie. Raspberry PI).
-   The log should say something like this once the migration is finished:

```
2017-11-19 13:46:29.416 INFO  [InfluxDataMigrator] Starting query threads...
2017-11-19 13:46:30.023 INFO  [InfluxDataMigrator] Processing...
2017-11-19 13:47:52.012 INFO  [InfluxDataMigrator] Finished migration! 912816 measurements migrated, took 82.823 seconds (11021.286357654275 measurements per second)
2017-11-19 13:47:52.016 INFO  [InfluxDataMigrator] accelerationX discarded: 0
2017-11-19 13:47:52.016 INFO  [InfluxDataMigrator] accelerationY discarded: 0
2017-11-19 13:47:52.016 INFO  [InfluxDataMigrator] accelerationZ discarded: 0
2017-11-19 13:47:52.016 INFO  [InfluxDataMigrator] battery discarded: 0
2017-11-19 13:47:52.017 INFO  [InfluxDataMigrator] humidity discarded: 0
2017-11-19 13:47:52.017 INFO  [InfluxDataMigrator] pressure discarded: 0
2017-11-19 13:47:52.017 INFO  [InfluxDataMigrator] rssi discarded: 0
2017-11-19 13:47:52.017 INFO  [InfluxDataMigrator] temperature discarded: 0
2017-11-19 13:47:52.478 INFO  [Main] Clean exit
```

-   Ideally you should have 0 discards like in the example above, and the number of measurements migrated should be the amount of measurements you had stored.
-   The migrator does _not_ delete the old measurements, so if something goes wrong, you can always try again
-   When you are happy with the result, you can continue running the collector normally like before, without the migrate argument
-   NOTE: as the format in InfluxDB changes, you need to update your applications accordingly (ie. Grafana, choose 'ruuvi_measurements' as the measurement and the desired type as the 'value')

### v0.1.2

-   Improved error handling
-   Added support for whitelist/blacklist filtering
-   Added support for dryrun mode

### v0.1.1

-   Support for protocol version 4
-   Bugfix related to protocol version 3 with latest weather-station firmware

## v0.1.0

-   First public release
