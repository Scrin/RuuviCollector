# Quickstart

To install RuuviCollector as a service and write collected data to an InfluxDB
database, you can run setup.sh from this directory after cloning the repo.

```sh
# Here's an example, you'd have to change the database URL, username, password,
# and database name accordingly to suit your deployment.
git clone https://github.com/Scrin/RuuviCollector
./RuuviCollector/service-setup/setup.sh -d "https://database.example.com:8086" -u ruuvi_user -p hunter2 -n ruuvitags
```

All files are installed into /opt/ruuvicollector

# Configuration file

The setup script won't overwrite /opt/ruuvicollector/ruuvi-collector.properties
if it exists, but it will update the configuration with any values passed in on
the command line.

If there is not an existing /opt/ruuvicollector/ruuvi-collector.properties file,
setup.sh will look for a ruuvi-collector.properties in the root of the git
checkout. If you are restoring from a backup or a version control system, you
would want to put your ruuvi-collector.properties there in order for it to be
installed.

If that is not present, which will typically be the case, setup.sh will copy
the example configuration to /opt/ruuvicollector/ruuvi-collector.properties and
update it as needed.
