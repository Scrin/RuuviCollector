# Quickstart

To install RuuviCollector as a service and write collected data to an InfluxDB
database, you can run setup.sh from this directory after cloning the repo.

## Single Instance Setup (Original Behavior)

Without the `-i` parameter, the script behaves exactly as before, installing to `/opt/ruuvicollector/`.

```sh
# Here's an example, you'd have to change the database URL, username, password,
# and database name accordingly to suit your deployment.
git clone https://github.com/Scrin/RuuviCollector
./RuuviCollector/service-setup/setup.sh -d "https://database.example.com:8086" -u ruuvi_user -p hunter2 -n ruuvitags
```

All files are installed into `/opt/ruuvicollector/`

## Multiple Instances Setup

You can run multiple instances of RuuviCollector, each with its own configuration and database.
To enable multi-instance mode, use the `-i` parameter to specify an instance name.
Each instance will have its own service file and configuration directory.

```sh
# Setup first instance
git clone https://github.com/Scrin/RuuviCollector
./RuuviCollector/service-setup/setup.sh -i instance1 -d "https://db1.example.com:8086" -n ruuvi1

# Setup second instance with different database
./RuuviCollector/service-setup/setup.sh -i instance2 -d "https://db2.example.com:8086" -n ruuvi2
```

Each instance will be installed into its own directory:
- Instance 1: /opt/ruuvicollector/instance1/
- Instance 2: /opt/ruuvicollector/instance2/

And each will have its own systemd service:
- ruuvicollector@instance1.service
- ruuvicollector@instance2.service

To manage individual instances:
```sh
# Start/stop/restart a specific instance
systemctl start ruuvicollector@instance1.service
systemctl stop ruuvicollector@instance1.service
systemctl restart ruuvicollector@instance1.service

# View logs for a specific instance
journalctl -u ruuvicollector@instance1.service -f

# Check status of a specific instance
systemctl status ruuvicollector@instance1.service
```

To manage the single instance (without `-i`):
```sh
# Start/stop/restart
systemctl start ruuvicollector
systemctl stop ruuvicollector
systemctl restart ruuvicollector

# View logs
journalctl -u ruuvicollector -f

# Check status
systemctl status ruuvicollector
```

Note: The lescan service is shared across all instances and only needs to be installed once.

# Configuration file

The setup script won't overwrite the configuration file if it exists, but it will update
the configuration with any values passed in on the command line.

For single instance mode (without `-i`), the configuration is at:
`/opt/ruuvicollector/ruuvi-collector.properties`

For multi-instance mode (with `-i`), the configuration is at:
`/opt/ruuvicollector/<instance>/ruuvi-collector.properties`

If there is not an existing configuration file,
setup.sh will look for a ruuvi-collector.properties in the root of the git
checkout. If you are restoring from a backup or a version control system, you
would want to put your ruuvi-collector.properties there in order for it to be
installed.

If that is not present, which will typically be the case, setup.sh will copy
the example configuration to the appropriate location and update it as needed.
