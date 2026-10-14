#!/bin/sh -e
#
# This script will compile ruuvicollector and set it up as a service. It will
# also set up a bluetooth scanning service, called "lescan".
#
# Usage:
#   ./setup.sh [-i INSTANCE] [-d DB_URL] [-u DB_USER] [-p DB_PASS] [-n DB_NAME]
#
#     - INSTANCE - Name of the instance (optional, enables multi-instance mode)
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
# located at /opt/ruuvicollector/ruuvi-collector.properties (or
# /opt/ruuvicollector/<instance>/ruuvi-collector.properties if -i is specified)
#
# To create multiple instances, run this script multiple times with different -i values.
# For example:
#   ./setup.sh -i instance1 -d "https://db1.example.com:8086" -n ruuvi1
#   ./setup.sh -i instance2 -d "https://db2.example.com:8086" -n ruuvi2
#
# Tested with Debian 11 (Raspberry Pi OS)
#

# Parse any provided arguments
INSTANCE=""
while getopts "i:d:u:p:n:" opt; do
    case $opt in
        i) INSTANCE="$OPTARG";;
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

# Determine installation directory based on whether -i was specified
if [ -n "$INSTANCE" ]; then
    # Multi-instance mode: use subdirectory
    INSTANCE_DIR="/opt/ruuvicollector/$INSTANCE"
    USE_TEMPLATE=1
else
    # Single instance mode: use original behavior
    INSTANCE_DIR="/opt/ruuvicollector"
    USE_TEMPLATE=0
fi

mkdir -p "$INSTANCE_DIR"
cp -r target/* "$INSTANCE_DIR"

# We do not overwrite existing configuration files
if [ ! -f "$INSTANCE_DIR/ruuvi-collector.properties" ]; then
    # If we have a ruuvi-collector.properties, we use that, otherwise we fall
    # back to the example configuration file
    if [ -f ruuvi-collector.properties ]; then
        cp ruuvi-collector.properties "$INSTANCE_DIR"
    else
        cp ruuvi-collector.properties.example \
           "$INSTANCE_DIR/ruuvi-collector.properties"
        # We will be running the scan command from a service, so we need to tell
        # RuuviCollector to not run this command itself.
        sed -i 's/[#]command\.scan=.*/command.scan=/' \
            "$INSTANCE_DIR/ruuvi-collector.properties"
        # We also want the service to restart if there's a database error,
        # however this requires also disabling batch mode (due to a limitation
        # of the InfluxDB library). Because there is a performance penalty, we
        # leave batch mode enabled, but we set RuuviCollector to exit if there
        # is a database error so it will happen if batch mode is later disabled
        # (or the InfluxDB library is updated to raise an exception when there's
        # a database error in batch mode).
        sed -i 's/[#]exitOnInfluxDBIOException=.*/exitOnInfluxDBIOException=true/' \
            "$INSTANCE_DIR/ruuvi-collector.properties"
    fi
fi

# Use database, username and password values, if they were set
if [ -n "$DB_URL" ]; then
    sed -i "s|[#]influxUrl=.*|influxUrl=$DB_URL|" \
        "$INSTANCE_DIR/ruuvi-collector.properties"
fi
if [ -n "$DB_USER" ]; then
    sed -i "s/[#]influxUser=.*/influxUser=$DB_USER/" \
        "$INSTANCE_DIR/ruuvi-collector.properties"
fi
if [ -n "$DB_PASS" ]; then
    sed -i "s|[#]influxPassword=.*|influxPassword=$DB_PASS|" \
        "$INSTANCE_DIR/ruuvi-collector.properties"
fi
if [ -n "$DB_NAME" ]; then
    sed -i "s/[#]influxDatabase=.*/influxDatabase=$DB_NAME/" \
        "$INSTANCE_DIR/ruuvi-collector.properties"
fi

# Copy ruuvi-names.properties if it exists (and does not exist in the destination)
if [ ! -f "$INSTANCE_DIR/ruuvi-names.properties" ] && [ -f ruuvi-names.properties ]; then
    cp ruuvi-names.properties "$INSTANCE_DIR"
fi

# Now to set up the services
# Copy lescan service (only once, not per-instance)
if [ ! -f /etc/systemd/system/lescan.service ]; then
    cp service-setup/lescan.service /etc/systemd/system
    systemctl enable lescan
    systemctl start lescan
fi

# Setup ruuvicollector service for this instance
if [ "$USE_TEMPLATE" -eq 1 ]; then
    # For named instances, use systemd template
    # Copy the template only if it doesn't exist
    if [ ! -f /etc/systemd/system/ruuvicollector@.service ]; then
        cp service-setup/ruuvicollector@.service /etc/systemd/system/ruuvicollector@.service
        systemctl daemon-reload
    fi
    # Create a symlink for this specific instance
    ln -sf /etc/systemd/system/ruuvicollector@.service /etc/systemd/system/ruuvicollector@${INSTANCE}.service
    systemctl daemon-reload
    systemctl enable ruuvicollector@${INSTANCE}.service
    systemctl start ruuvicollector@${INSTANCE}.service
    INSTANCE_MSG="instance '$INSTANCE'"
else
    # For backward compatibility, support the old non-templated service
    cp service-setup/ruuvicollector.service /etc/systemd/system/ruuvicollector.service
    systemctl enable ruuvicollector
    systemctl start ruuvicollector
    INSTANCE_MSG="RuuviCollector"
fi

# And the watchdog service (only once, not per-instance)
if [ ! -f /etc/systemd/system/lescan_watchdog.timer ]; then
    cp service-setup/lescan_watchdog.service service-setup/lescan_watchdog.timer /etc/systemd/system
    cp service-setup/lescan_watchdog.sh /usr/local/bin/
    chmod +x /usr/local/bin/lescan_watchdog.sh
    systemctl enable lescan_watchdog.timer
    systemctl start lescan_watchdog.timer
fi

if [ "$USE_TEMPLATE" -eq 1 ]; then
    echo "RuuviCollector $INSTANCE_MSG has been set up successfully."
    echo "Service status: systemctl status ruuvicollector@${INSTANCE}.service"
    echo "Logs: journalctl -u ruuvicollector@${INSTANCE}.service -f"
else
    echo "$INSTANCE_MSG has been set up successfully."
    echo "Service status: systemctl status ruuvicollector"
    echo "Logs: journalctl -u ruuvicollector -f"
fi
