# RuuviCollector

RuuviCollector is an application for collecting sensor measurements from RuuviTags and storing them to InfluxDB. For more about how and for what this is used for, see [this](https://f.ruuvi.com/t/collecting-ruuvitag-measurements-and-displaying-them-with-grafana/267) post.

Note: As this is the first release of this application, there is very little documentation at this point, so some knowledge in Linux and Java is necessary for fully understanding how to use this at this point.

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

- Run the built JAR-file with `java -jar ruuvi-collector-0.1.jar`. Note: as there is no service scripts yet, it's recommended to run this for example inside *screen* to avoid the application being killed when terminal session ends
- To configure the settings, copy the `ruuvi-collector.properties.example` to `ruuvi-collector.properties` and place it in the same directory as the JAR file and edit the file according to your needs.

### Running

For built version (while in the "root" of the project):

```sh
java -jar target/ruuvi-collector-0.1.jar
```

Easily compile and run while developing:

```
mvn compile exec:java
```
