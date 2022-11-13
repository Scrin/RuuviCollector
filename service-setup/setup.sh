#!/bin/sh -e
#
# This script will compile ruuvicollector and set it up as a service. It will
# also set up a bluetooth scanning service, called "lescan".
#
# Usage:
#   ./setup.sh [-d DB_URL] [-u DB_USER] [-p DB_PASS] [-n DB_NAME]
#
#     - DB_URL - URL for the InfluxDB where data should be stored
#     - DB_USER - username to use when connecting to the database
#     - DB_PASS - password to use when connecting to the database
#     - DB_NAME - database name to use when connecting to InfluxDB
#
# Note on passwords:
# If your password contains a pipe (|), dollar sign ($) or double quote ("), you
# will have to edit the configuration manually. The setup script does not handle
# these characters properly.
#
# Configuration files:
# Any arguments passed in on the command line will be applied to the
# configuration file.
#
# If there is a ruuvi-collector.properties file in the root of the checkout,
# that will be used as is. Otherwise, a configuration file based on the example
# will be used. In any case, the configuration file used by the service will be
# located at /opt/ruuvicollector/ruuvi-collector.properties
#
# Tested with Debian 11 (Raspberry Pi OS)
#

# Parse any provided arguments
while getopts "d:u:p:n:" opt; do
    case $opt in
        d) DB_URL="$OPTARG";;
        u) DB_USER="$OPTARG";;
        p) DB_PASS="$OPTARG";;
        n) DB_NAME="$OPTARG";;
    esac
done

# Permission check
if [ "$USER" != "root" ]; then
	echo "Error: installation script must be run as root (e.g. sudo)"
	exit 1
fi

# We want to make our working directory be one above where this script is
# located, which may or may not be one directory up from the current working
# directory.
abspath=`realpath $0`
scriptpath=`dirname $abspath`
cd $scriptpath/..
pwd

apt update
apt install -y maven bluez bluez-hcidump
# Install the latest JDK that is available
apt install -y openjdk-17-jdk-headless || \
    apt install -y openjdk-11-jdk-headless || \
    apt install -y openjdk-8-jdk-headless
mvn clean package

mkdir -p /opt/ruuvicollector
cp -r target/* /opt/ruuvicollector
# We do not overwrite existing configuration files
if [ ! -f /opt/ruuvicollector/ruuvi-collector.properties ]; then
    # If we have a ruuvi-collector.properties, we use that, otherwise we fall
    # back to the example configuration file
    if [ -f ruuvi-collector.properties ]; then
        cp ruuvi-collector.properties /opt/ruuvicollector
    else
        cp ruuvi-collector.properties.example \
           /opt/ruuvicollector/ruuvi-collector.properties
        # We will be running the scan command from a service, so we need to tell
        # RuuviCollector to not run this command itself.
        sed -i 's/[#]command\.scan=.*/command.scan=/' \
            /opt/ruuvicollector/ruuvi-collector.properties
        # We also want the service to restart if there's a database error,
        # however this requires also disabling batch mode (due to a limitation
        # of the InfluxDB library). Because there is a performance penalty, we
        # leave batch mode enabled, but we set RuuviCollector to exit if there
        # is a database error so it will happen if batch mode is later disabled
        # (or the InfluxDB library is updated to raise an exception when there's
        # a database error in batch mode).
        sed -i 's/[#]exitOnInfluxDBIOException=.*/exitOnInfluxDBIOException=true/' \
            /opt/ruuvicollector/ruuvi-collector.properties
    fi
fi

# Use database, username and password values, if they were set
if [ -n "$DB_URL" ]; then
    sed -i "s|[#]influxUrl=.*|influxUrl=$DB_URL|" \
        /opt/ruuvicollector/ruuvi-collector.properties
fi
if [ -n "$DB_USER" ]; then
    sed -i "s/[#]influxUser=.*/influxUser=$DB_USER/" \
        /opt/ruuvicollector/ruuvi-collector.properties
fi
if [ -n "$DB_PASS" ]; then
    sed -i "s|[#]influxPassword=.*|influxPassword=$DB_PASS|" \
        /opt/ruuvicollector/ruuvi-collector.properties
fi
if [ -n "$DB_NAME" ]; then
    sed -i "s/[#]influxDatabase=.*/influxDatabase=$DB_NAME/" \
        /opt/ruuvicollector/ruuvi-collector.properties
fi

# Copy ruuvi-names.properties if it exists (and does not exist in the destination)
if [ ! -f /opt/ruuvicollector/ruuvi-names.properties ] && [ -f ruuvi-names.properties ]; then
    cp ruuvi-names.properties /opt/ruuvicollector
fi

# Now to set up the services
cp service-setup/lescan.service service-setup/ruuvicollector.service /etc/systemd/system
systemctl enable lescan ruuvicollector
systemctl start lescan ruuvicollector
# And the watchdog service
cp service-setup/lescan_watchdog.service service-setup/lescan_watchdog.timer /etc/systemd/system
cp service-setup/lescan_watchdog.sh /usr/local/bin/
sudo systemctl enable lescan_watchdog.timer
sudo systemctl start lescan_watchdog.timer
