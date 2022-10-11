#!/bin/sh
if [ `journalctl -eu lescan --since "1 minute ago" | wc -l` -le 2 ]; then
	systemctl restart lescan
fi
