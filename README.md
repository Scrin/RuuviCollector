# RuuviCollector

RuuviCollector is an application for collecting sensor measurements from RuuviTags and storing them to InfluxDB. For more about how and for what this is used for, see [this](https://f.ruuvi.com/t/collecting-ruuvitag-measurements-and-displaying-them-with-grafana/267) post.

Note: As this is the first release of this application, there is very little documentation at this point, so some knowledge in Linux and Java is necessary for fully understanding how to use this at this point.

### Features

Supports following RuuviTag [Data Formats](https://github.com/ruuvi/ruuvi-sensor-protocols):

 - Data Format 2: Eddystone-URL, URL-safe base64 -encoded, kickstarter edition
 - Data Format 3: BLE Manufacturer specific data, all current sensor readings
 - Data Format 4: Eddystone-URL, URL-safe base64 -encoded, with tag id.

Supports following data from the tag (depending on tag firmware):

 - Temperature (Celcsius)
 - Relative humidity (0-100%)
 - Air pressure (Pascal)
 - Acceleration for X, Y and Z axes (g)
 - Battery voltage (Volts)
 - RSSI (Signal strength *at the receiver*, dBm)

Ability to calculate following values in addition to the raw data (the accuracy of these values are approximations):

 - Total acceleration (g)
 - Absolute humidity (g/m³)
 - Dew point (Celsius)
 - Equilibrium vapor pressure (Pascal)
 - Air density (Accounts for humidity in the air, kg/m³)
 - Acceleration angle from X, Y and Z axes (Degrees)

### Requirements

* Linux-based OS (this application uses the bluez stack for Bluetooth which is not available for Windows for example)
* Bluetooth adapter supporting Bluetooth Low Energy
* *bluez* and *bluez-hcidump* at least version 5.41 (For running the application, versions prior to 5.41 have a bug which causes the bluetooth to hang occasionally while doing BLE scanning)
* Maven (For building from sources)
* JDK8 (For building from sources, JRE8 is enough for just running the built JAR)

### Building

Execute 

```sh
mvn clean package
```

### Installation

TODO: Service scripts and other necessary stuff for "properly installing" this will be added later.
For now, you can do the following to the this up and running:

- hcitool and hcidump require additional capabilities, so you need to execute the following commands or run the application as root

```sh
sudo setcap 'cap_net_raw,cap_net_admin+eip' `which hcitool`
sudo setcap 'cap_net_raw,cap_net_admin+eip' `which hcidump`
```

- Run the built JAR-file with `java -jar ruuvi-collector-*.jar`. Note: as there is no service scripts yet, it's recommended to run this for example inside *screen* to avoid the application being killed when terminal session ends
- To configure the settings, copy the `ruuvi-collector.properties.example` to `ruuvi-collector.properties` and place it in the same directory as the JAR file and edit the file according to your needs.

### Configuration

The default configuration which works without a config file assumes InfluxDB is running locally with default settings, with a database called 'ruuvi'.
To change the default settings, copy the ruuvi-collector.properties.example file as ruuvi-collector.properties in the same directory as the collector jar file and change the settings you want.
Most up-to-date information about the supported configuration options can be found in the ruuvi-collector.properties.example file.

To give human readable friendly names to tags (based on their MAC address), copy the ruuvi-names.properties.example file as ruuvi-names.properties in the same directory as the collector jar file and set the names in this file according to the examples there.

### Running

For built version (while in the "root" of the project):

```sh
java -jar target/ruuvi-collector-*.jar
```

Easily compile and run while developing:

```
mvn compile exec:java
```
