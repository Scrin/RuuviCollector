# Measurements

Saving and handling the measurements in the RuuviCollector is explained here. See the [Data Formats](https://github.com/ruuvi/ruuvi-sensor-protocols) for format-specific details regarding the resolution data and valid values.

### Saving measurements

Unless manually configured otherwise, the collector will always store each received measurement independently at the time of receiving it regardless of the data format of a particular measurement (depending on tag firmware), unless limited by the update interval (configurable, 9.9 seconds by default), which means up to one measurement per tag and per data format is persisted per 9.9 seconds.

Example: if a tag transmits every 1 seconds; after receiving (and saving) the first measurement, the next 9 measurements will be discarded and the 10th measurement (arriving about 10 seconds after the first) will be saved, unless it's missed in which case the 11th (11 seconds later) is saved. The time limit per tag per data format "resets" only up on saving such measurement, which means the interval will eventually start wandering off relative to "wall clock time".

The purpose of this default limit is to reduce the load on very low-end systems listening to multiple tags. If a strict "wall clock interval" is required, the collector should be configured to store all measurements (set the update limit to 0) and configure an appropriate retention policy and/or continuous query to InfluxDB to remove/aggregate values as needed.

For maximum accuracy, the update interval should be configured to 0 to save every single received measurement without discarding anything.

### Data format compatibility

Note: Extended values are enabled by default but can be disabled if only actual values from the tag are needed.

| Type                            | Unit (saved)         | Data format 2 | Data format 3 | Data format 4 | Data format 5 |
| ------------------------------- | -------------------- | ------------- | ------------- | ------------- | ------------- |
| Temperature                     | Celsius              | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Relative humidity               | Percent (0-100)      | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Air pressure                    | Pascal               | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Acceleration (x, y ,z)          | Gravity of earth (g) | No            | **Yes**       | No            | **Yes**       |
| Battery voltage                 | Volt                 | No            | **Yes**       | No            | **Yes**       |
| TX power                        | dBm                  | No            | No            | No            | **Yes**       |
| RSSI                            | dBm                  | **Yes** *(1)* |**Yes** *(1)*  | **Yes** *(1)* | **Yes** *(1)* |
| Movement counter                | Number               | No            | No            | No            | **Yes**       |
| Measurement sequence number     | Number               | No            | No            | No            | **Yes**       |
| **Extended values, if enabled** |                      |               |               |               |               |
| Total acceleration              | Gravity of earth (g) | No            | **Yes**       | No            | **Yes**       |
| Absolute humidity               | g/m³                 | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Dew point                       | Celsius              | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Equilibrium vapor pressure      | Pascal               | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Air density                     | kg/m³                | **Yes**       | **Yes**       | **Yes**       | **Yes**       |
| Acceleration angle from axes    | Degrees              | No            | **Yes**       | No            | **Yes**       |

*(1)* RSSI is the (relative) signal strength at the receiver, it is not dependent on tag firmware.

### Calculating extended values

All extended values are calculated in [MeasurementValueCalculator.java](./src/main/java/fi/tkgwf/ruuvi/utils/MeasurementValueCalculator.java). Unfortunately I've lost the original sources for most of the equations, but with a little searching similar ones can be found, such as [this](https://www.vaisala.com/sites/default/files/documents/Humidity_Conversion_Formulas_B210973EN-F.pdf). Needless to say, the extended values are rough approximations, sufficient for the average home user. For scientific use, you should disable the extended values and use your own scientifically accurate formulas.
